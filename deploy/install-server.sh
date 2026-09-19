#!/usr/bin/env bash
#
# =============================================================================
# Korrupsiyaga qarshi kurash portali - serverni tayyorlash (Ubuntu 24.04 LTS).
#
# Nima qiladi:
#   - Java 21 (JRE), PostgreSQL, nginx o'rnatadi
#   - `anticorruption` tizim foydalanuvchisi va papkalarni yaratadi
#   - baza va uning egasini yaratadi
#   - maxfiy qiymatlarni /etc/anticorruption/env ga yozadi (huquqi 600)
#   - systemd xizmati va nginx saytini o'rnatadi
#
# Nima QILMAYDI: ilovani ishga tushirmaydi - jar va frontend hali yo'q.
# Ularni yuklagandan keyin:  systemctl start anticorruption
#
# Ishga tushirish:  sudo bash install-server.sh
#
# Skript qayta-qayta ishga tushirilishi mumkin: mavjud baza, foydalanuvchi
# va maxfiy qiymatlar qayta yaratilmaydi. Maxfiy qiymatlar bir marta
# hosil qilinadi - aks holda har ishga tushirishda seanslar uzilardi.
# =============================================================================

set -euo pipefail

APP_USER=anticorruption
APP_DIR=/opt/anticorruption
WEB_DIR=/var/www/anticorruption
DATA_DIR=/var/lib/anticorruption/uploads
LOG_DIR=/var/log/anticorruption
ENV_DIR=/etc/anticorruption
ENV_FILE="$ENV_DIR/env"
DB_NAME=anticorruption
DB_USER=anticorruption

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }

if [ "$(id -u)" -ne 0 ]; then
    echo "Bu skript root huquqi bilan ishlashi kerak:  sudo bash $0" >&2
    exit 1
fi

# -----------------------------------------------------------------------------
# 1. Paketlar
#
# JDK emas, JRE: serverda kompilyatsiya qilinmaydi, faqat tayyor jar ishga
# tushiriladi. PostgreSQL Ubuntu omboridan olinadi (24.04 da 16-versiya) -
# migratsiyalar oddiy SQL, tashqi ombor kerak emas.
# -----------------------------------------------------------------------------
log "Paketlar o'rnatilmoqda"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq \
    openjdk-21-jre-headless \
    postgresql postgresql-contrib \
    nginx \
    ufw openssl ca-certificates

systemctl enable --now postgresql
systemctl enable --now nginx

# -----------------------------------------------------------------------------
# 2. Foydalanuvchi va papkalar
#
# Ilova o'z nomidan ishlaydi: root bo'lsa, undagi har qanday zaiflik butun
# serverni ochib qo'yardi. Kirish huquqi yo'q (nologin) - bu hisob orqali
# tizimga kirilmaydi.
# -----------------------------------------------------------------------------
log "Foydalanuvchi va papkalar"
if ! id -u "$APP_USER" >/dev/null 2>&1; then
    useradd --system --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
    echo "  $APP_USER yaratildi"
else
    echo "  $APP_USER allaqachon bor"
fi

mkdir -p "$APP_DIR" "$WEB_DIR" "$DATA_DIR" "$LOG_DIR" "$ENV_DIR"
# Yuklangan fayllar va loglar ilovaniki; qolganini root boshqaradi.
chown -R "$APP_USER:$APP_USER" "$DATA_DIR" "$LOG_DIR"
chmod 750 "$DATA_DIR" "$LOG_DIR"
chmod 750 "$ENV_DIR"

# -----------------------------------------------------------------------------
# 3. Maxfiy qiymatlar
#
# Faqat bir marta hosil qilinadi. JWT kaliti almashsa - barcha seanslar
# uziladi; ovoz tuzi almashsa - ilgari ovoz berganlar qayta ovoz bera oladi.
# Qiymatlar o'n oltilik (hex): systemd env faylida maxsus belgilar bilan
# bog'liq muammo bo'lmasin.
# -----------------------------------------------------------------------------
log "Maxfiy qiymatlar"
if [ ! -f "$ENV_FILE" ]; then
    DB_PASSWORD="$(openssl rand -hex 24)"
    JWT_SECRET="$(openssl rand -hex 32)"
    POLL_SALT="$(openssl rand -hex 24)"
    ADMIN_PASSWORD="$(openssl rand -hex 12)"
    MODERATOR_PASSWORD="$(openssl rand -hex 12)"
    SERVER_IP="$(hostname -I | awk '{print $1}')"

    cat > "$ENV_FILE" <<EOF
# Maxfiy qiymatlar. Bu faylni nusxalamang va repoga qo'ymang.
# Huquqi 600, egasi root.

DB_URL=jdbc:postgresql://localhost:5432/$DB_NAME
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD

APP_JWT_SECRET=$JWT_SECRET
APP_POLL_SALT=$POLL_SALT

# Birinchi ishga tushishda shu administrator yaratiladi.
# Kirgandan keyin parolni almashtiring - u shu faylda ochiq turadi.
APP_ADMIN_EMAIL=admin@anticorruption.uz
APP_ADMIN_PASSWORD=$ADMIN_PASSWORD

# Moderator ham shu yerda beriladi. Berilmasa application.properties dagi
# standart "Moderator12345!" ishlatiladi - u README da yozilgan va omma
# uchun ma'lum, ya'ni hisob amalda himoyalanmagan bo'lardi.
APP_MODERATOR_EMAIL=moderator@anticorruption.uz
APP_MODERATOR_PASSWORD=$MODERATOR_PASSWORD

# Yuklangan fayllar jar bilan bir joyda emas: yangilash ularni o'chirmasin.
APP_STORAGE_LOCATION=$DATA_DIR
LOG_FILE=$LOG_DIR/anticorruption.log

# ---------------------------------------------------------------------------
# HTTPS hali yo'q.
#
# Cookie secure=true bo'lsa brauzer uni HTTP orqali yubormaydi va tizimga
# kirish ishlamaydi. Shuning uchun hozircha false va `prod` profili
# YOQILMAGAN: prod da ProductionSafetyCheck aynan shu sozlama uchun
# ilovani ishga tushirmaydi.
#
# Domen va sertifikat paydo bo'lgach:
#   1) APP_COOKIE_SECURE=true qiling
#   2) APP_CORS_ORIGINS ga haqiqiy domenni yozing
#   3) quyidagi qatorni qo'shing:  SPRING_PROFILES_ACTIVE=prod
#   4) systemctl restart anticorruption
# ---------------------------------------------------------------------------
APP_COOKIE_SECURE=false
APP_CORS_ORIGINS=http://$SERVER_IP
EOF
    echo "  $ENV_FILE yaratildi"
else
    echo "  $ENV_FILE allaqachon bor - tegilmadi"
fi

chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"

# -----------------------------------------------------------------------------
# 4. Ma'lumotlar bazasi
#
# Sxemani Flyway yaratadi - bu yerda faqat bo'sh baza va uning egasi.
# -----------------------------------------------------------------------------
log "Ma'lumotlar bazasi"
DB_PASSWORD="$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
    sudo -u postgres psql -qc "CREATE ROLE $DB_USER LOGIN"
    echo "  $DB_USER roli yaratildi"
fi
# Parol har doim env fayldagisiga tenglashtiriladi - ikkisi ajralib
# qolsa ilova bazaga ulana olmasdi.
sudo -u postgres psql -qc "ALTER ROLE $DB_USER WITH PASSWORD '$DB_PASSWORD'"

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1; then
    sudo -u postgres createdb -O "$DB_USER" "$DB_NAME"
    echo "  $DB_NAME bazasi yaratildi"
else
    echo "  $DB_NAME allaqachon bor"
fi

# -----------------------------------------------------------------------------
# 5. systemd xizmati
#
# `prod` profili bu yerda ko'rsatilmagan - u env faylidagi
# SPRING_PROFILES_ACTIVE orqali yoqiladi (yuqoridagi izohga qarang).
# -----------------------------------------------------------------------------
log "systemd xizmati"
cat > /etc/systemd/system/anticorruption.service <<EOF
[Unit]
Description=Korrupsiyaga qarshi kurash portali
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$APP_DIR
EnvironmentFile=$ENV_FILE
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=60 -jar $APP_DIR/anticorruption.jar
# Spring Boot SIGTERM da 143 qaytaradi - bu xato emas.
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
TimeoutStopSec=30

# Ilovaga kerak bo'lmagan joylar yopiladi.
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=$DATA_DIR $LOG_DIR

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable anticorruption >/dev/null
echo "  anticorruption.service o'rnatildi (hali ishga tushirilmadi)"

# -----------------------------------------------------------------------------
# 6. nginx
#
# Frontend statik fayl sifatida beriladi, /api esa ilovaga uzatiladi.
# Domen yo'q - sayt IP bo'yicha ochiladi (default_server).
# -----------------------------------------------------------------------------
log "nginx"
cat > /etc/nginx/sites-available/anticorruption <<'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    root /var/www/anticorruption;
    index index.html;

    # Brauzer manzil satridan ochilgan har qanday yo'l React'ga beriladi:
    # marshrutlarni server emas, ilovaning o'zi hal qiladi.
    location / {
        try_files $uri /index.html;
    }

    # Xesh nomli fayllar hech qachon o'zgarmaydi - uzoq keshlanadi.
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        # Tashqaridan kelgan qiymat almashtiriladi: aks holda so'rov
        # chegarasini soxta manzil bilan aldab bo'lardi.
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        # Yangilik albomiga bir nechta rasm birdan yuklanadi.
        client_max_body_size 60m;
    }

    gzip on;
    gzip_types text/css application/javascript application/json image/svg+xml;
    gzip_min_length 1024;
}
EOF

ln -sf /etc/nginx/sites-available/anticorruption /etc/nginx/sites-enabled/anticorruption
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
echo "  nginx sayti yoqildi"

# -----------------------------------------------------------------------------
# 7. Devor
#
# 8080 ataylab ochilmaydi: ilovaga faqat nginx orqali kiriladi.
# -----------------------------------------------------------------------------
log "Devor (ufw)"
ufw allow OpenSSH >/dev/null
ufw allow 80/tcp >/dev/null
ufw --force enable >/dev/null
ufw status | head -6

# -----------------------------------------------------------------------------
log "Server tayyor"
cat <<EOF

Keyingi qadam - ilovani yuklash:

  scp backend/target/anticorruption-0.0.1-SNAPSHOT.jar \\
      rttm@SERVER:/tmp/anticorruption.jar
  scp -r frontend/dist/* rttm@SERVER:/tmp/dist/

  sudo mv /tmp/anticorruption.jar $APP_DIR/anticorruption.jar
  sudo chown $APP_USER:$APP_USER $APP_DIR/anticorruption.jar
  sudo cp -r /tmp/dist/* $WEB_DIR/
  sudo systemctl start anticorruption

Hisob parollari (birinchi kirishdan keyin almashtiring):

  sudo grep -E 'APP_(ADMIN|MODERATOR)_PASSWORD' $ENV_FILE

EOF
