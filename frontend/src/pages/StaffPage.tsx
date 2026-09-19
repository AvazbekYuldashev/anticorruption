import { useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/content';
import type { StaffMemberResponse } from '../api/types';
import { EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';
import { renderRichText } from '../lib/richText';

/** Ism bosh harflari - surat bo'lmaganda o'rniga ko'rsatiladi. */
function initials(fullName: string): string {
  return fullName
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

/**
 * Ma'lumot qatori: chapda kichik yozuvli nom, o'ngda qiymat, ostida ingichka
 * ajratkich. Qatorlar kartochka tubiga tekislanadi - qo'shni kartochkada
 * biografiya uzunroq bo'lsa ham ular bir sathda turadi.
 */
function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 py-2.5">
      <dt className="shrink-0 text-[11px] font-medium tracking-wider text-slate-400 dark:text-slate-500 uppercase">
        {label}
      </dt>
      <dd className="min-w-0 truncate text-right text-sm font-semibold text-slate-800 dark:text-slate-200">
        {children}
      </dd>
    </div>
  );
}

function StaffCard({ member }: { member: StaffMemberResponse }) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  const bio = member.biography?.trim();

  /*
    "Batafsil" tugmasi matn haqiqatan ham sig'masagina chiqadi. Belgilar sonini
    sanash aldaydi: qatorlar soni shrift va kartochka eniga bog'liq, shuning
    uchun brauzer o'lchagan balandlik solishtiriladi. Matn ochilganda qayta
    o'lchanmaydi - aks holda tugma o'zi yo'qolib, matnni yopib bo'lmay qolardi.
  */
  const bioRef = useRef<HTMLParagraphElement>(null);
  const [clipped, setClipped] = useState(false);

  useLayoutEffect(() => {
    if (expanded) return;
    const el = bioRef.current;
    if (el) setClipped(el.scrollHeight > el.clientHeight + 1);
  }, [bio, expanded]);

  return (
    <article className="flex flex-col overflow-hidden rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm transition-shadow hover:shadow-md sm:flex-row">
      {/*
        Surat chapda, kartochka balandligicha cho'ziladi. Portret suratlar
        turli o'lchamda keladi, shuning uchun object-cover: ramka doim bir xil,
        surat esa kesiladi-yu, cho'zilib buzilmaydi.
      */}
      {member.photoUrl ? (
        <img
          src={member.photoUrl}
          alt=""
          className="h-56 w-full object-cover object-top sm:h-auto sm:w-[45%] sm:shrink-0"
        />
      ) : (
        <div className="flex h-56 w-full items-center justify-center bg-brand-50 dark:bg-brand-500/15 text-3xl font-semibold text-brand-700 dark:text-brand-300 sm:h-auto sm:w-[45%] sm:shrink-0">
          {initials(member.fullName)}
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col p-6">
        <p className="truncate text-center text-sm text-slate-600 dark:text-slate-400">{member.position}</p>
        <h2 className="mt-1 truncate text-center text-xl font-bold text-slate-900 dark:text-slate-100">
          {member.fullName}
        </h2>
        {member.academicDegree && (
          <p className="mt-1 truncate text-center text-xs text-slate-500 dark:text-slate-400">
            {member.academicDegree}
          </p>
        )}

        {/*
          Biografiya uch qatorgacha yig'iladi: kartochka baland bo'lib ketmasin -
          aks holda chapdagi surat ingichka lentaga aylanadi.
        */}
        {bio && (
          <div className="mt-4">
            <p
              ref={bioRef}
              className={`text-sm leading-6 whitespace-pre-line text-slate-600 dark:text-slate-400 ${
                expanded ? '' : 'line-clamp-3'
              }`}
            >
              {renderRichText(bio)}
            </p>
            {clipped && (
              <button
                type="button"
                onClick={() => setExpanded(!expanded)}
                className="mt-1 text-sm font-medium text-brand-600 dark:text-brand-300 hover:underline"
              >
                {expanded ? t('staff.less') : t('staff.more')}
              </button>
            )}
          </div>
        )}

        <dl className="mt-auto divide-y divide-slate-100 dark:divide-slate-800 pt-5">
          {member.receptionHours && (
            <InfoRow label={t('staff.reception')}>{member.receptionHours}</InfoRow>
          )}
          {member.phone && (
            <InfoRow label={t('staff.phone')}>
              <a href={`tel:${member.phone}`} className="hover:text-brand-600 dark:hover:text-brand-300">
                {member.phone}
              </a>
            </InfoRow>
          )}
          {member.email && (
            <InfoRow label={t('staff.email')}>
              <a href={`mailto:${member.email}`} className="hover:text-brand-600 dark:hover:text-brand-300">
                {member.email}
              </a>
            </InfoRow>
          )}
        </dl>
      </div>
    </article>
  );
}

export function StaffPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['staff'], queryFn: contentApi.staff });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  return (
    <div>
      <PageHeader title={t('staff.title')} description={t('staff.intro')} />

      {query.data.length === 0 ? (
        <EmptyState message={t('staff.empty')} />
      ) : (
        /*
          Ikki ustun: kartochka gorizontal (surat + matn), shuning uchun uchta
          ustunda matn qismi juda torayib ketadi.
        */
        <div className="grid gap-6 lg:grid-cols-2">
          {query.data.map((member) => (
            <StaffCard key={member.id} member={member} />
          ))}
        </div>
      )}
    </div>
  );
}
