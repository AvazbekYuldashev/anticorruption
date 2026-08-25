package api.anticorruption.common.i18n;

/**
 * Barcha xabar kalitlari bir joyda.
 *
 * <p>Kalitlarni satr sifatida yozish o'rniga konstanta ishlatiladi: xato yozilgan
 * kalit kompilyatsiyada emas, faqat ish paytida - foydalanuvchi ekranida xom
 * "error.some.typo" ko'rinishida chiqardi. Bu ro'yxat ayni paytda tarjimonlar
 * uchun ham qo'llanma bo'lib xizmat qiladi.
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    // ---------------------------------------------------------------- umumiy

    public static final String ERROR_INTERNAL = "error.internal";
    public static final String ERROR_VALIDATION_FAILED = "error.validation.failed";
    public static final String ERROR_REQUEST_UNREADABLE = "error.request.unreadable";
    public static final String ERROR_REQUEST_PARAMETER_INVALID = "error.request.parameterInvalid";
    public static final String ERROR_REQUEST_PARAMETER_MISSING = "error.request.parameterMissing";

    // ---------------------------------------------------------------- huquq

    public static final String ERROR_AUTH_REQUIRED = "error.auth.required";
    public static final String ERROR_AUTH_BAD_CREDENTIALS = "error.auth.badCredentials";
    public static final String ERROR_AUTH_ACCOUNT_DISABLED = "error.auth.accountDisabled";
    public static final String ERROR_AUTH_EMAIL_TAKEN = "error.auth.emailTaken";
    public static final String ERROR_ACCESS_DENIED = "error.access.denied";

    // ---------------------------------------------------------------- topilmadi

    public static final String NOT_FOUND_COMPLAINT = "error.notFound.complaint";
    public static final String NOT_FOUND_USER = "error.notFound.user";
    public static final String NOT_FOUND_STAFF_USER = "error.notFound.staffUser";
    public static final String NOT_FOUND_FACULTY = "error.notFound.faculty";
    public static final String NOT_FOUND_DEPARTMENT = "error.notFound.department";
    public static final String NOT_FOUND_ATTACHMENT = "error.notFound.attachment";
    public static final String NOT_FOUND_NEWS = "error.notFound.news";
    public static final String NOT_FOUND_PAGE = "error.notFound.page";
    public static final String NOT_FOUND_STAFF_MEMBER = "error.notFound.staffMember";
    public static final String NOT_FOUND_LINK = "error.notFound.link";
    public static final String NOT_FOUND_POLL = "error.notFound.poll";
    public static final String NOT_FOUND_NEWS_IMAGE = "error.notFound.newsImage";

    // ---------------------------------------------------------------- murojaat

    public static final String COMPLAINT_NOT_FOUND_BY_CODE = "error.complaint.notFoundByCode";
    public static final String COMPLAINT_ALREADY_IN_STATUS = "error.complaint.alreadyInStatus";
    public static final String COMPLAINT_TRANSITION_NOT_ALLOWED = "error.complaint.transitionNotAllowed";
    public static final String COMPLAINT_ASSIGNEE_MUST_BE_STAFF = "error.complaint.assigneeMustBeStaff";
    public static final String COMPLAINT_ASSIGNEE_DISABLED = "error.complaint.assigneeDisabled";
    public static final String COMPLAINT_ALREADY_HIDDEN = "error.complaint.alreadyHidden";
    public static final String COMPLAINT_ALREADY_VISIBLE = "error.complaint.alreadyVisible";
    public static final String COMPLAINT_ACCEPTED = "message.complaint.accepted";

    // ---------------------------------------------------------------- tuzilma

    public static final String FACULTY_CODE_TAKEN = "error.faculty.codeTaken";
    public static final String FACULTY_IN_USE = "error.faculty.inUse";
    public static final String FACULTY_INACTIVE = "error.faculty.inactive";
    public static final String DEPARTMENT_CODE_TAKEN = "error.department.codeTaken";
    public static final String DEPARTMENT_IN_USE = "error.department.inUse";
    public static final String DEPARTMENT_INACTIVE = "error.department.inactive";
    public static final String DEPARTMENT_FACULTY_MISMATCH = "error.department.facultyMismatch";

    // ---------------------------------------------------------------- foydalanuvchi

    public static final String USER_ROLE_REQUIRED = "error.user.roleRequired";
    public static final String USER_CANNOT_CHANGE_OWN_ROLE = "error.user.cannotChangeOwnRole";
    public static final String USER_ALREADY_IN_ROLE = "error.user.alreadyInRole";
    public static final String USER_CANNOT_DISABLE_SELF = "error.user.cannotDisableSelf";
    public static final String USER_ALREADY_ENABLED = "error.user.alreadyEnabled";
    public static final String USER_ALREADY_DISABLED = "error.user.alreadyDisabled";

    // ---------------------------------------------------------------- fayllar

    public static final String FILE_EMPTY = "error.file.empty";
    public static final String FILE_NONE_SELECTED = "error.file.noneSelected";
    public static final String FILE_TYPE_NOT_ALLOWED = "error.file.typeNotAllowed";
    public static final String FILE_IMAGE_TYPE_NOT_ALLOWED = "error.file.imageTypeNotAllowed";
    public static final String FILE_INVALID_NAME = "error.file.invalidName";
    public static final String FILE_NOT_FOUND = "error.file.notFound";
    public static final String FILE_UNREADABLE = "error.file.unreadable";
    public static final String FILE_TOO_LARGE = "error.file.tooLarge";
    public static final String ATTACHMENT_COMPLAINT_CLOSED = "error.attachment.complaintClosed";
    public static final String ATTACHMENT_TOO_MANY = "error.attachment.tooMany";

    // ---------------------------------------------------------------- kontent

    public static final String NEWS_NOT_FOUND_BY_SLUG = "error.news.notFoundBySlug";
    public static final String NEWS_ALREADY_PUBLISHED = "error.news.alreadyPublished";
    public static final String NEWS_ALREADY_DRAFT = "error.news.alreadyDraft";
    public static final String NEWS_TOO_MANY_IMAGES = "error.news.tooManyImages";
    public static final String NEWS_BLOCK_TYPE_INVALID = "error.news.blockTypeInvalid";
    public static final String NEWS_TEXT_BLOCK_EMPTY = "error.news.textBlockEmpty";
    public static final String NEWS_IMAGE_BLOCK_MISSING = "error.news.imageBlockMissing";
    public static final String NEWS_GALLERY_BLOCK_EMPTY = "error.news.galleryBlockEmpty";
    public static final String PAGE_SLUG_TAKEN = "error.page.slugTaken";
    public static final String PAGE_NOT_FOUND_BY_SLUG = "error.page.notFoundBySlug";

    // ---------------------------------------------------------------- so'rovnoma

    public static final String POLL_CLOSED = "error.poll.closed";
    public static final String POLL_ALREADY_VOTED = "error.poll.alreadyVoted";
    public static final String POLL_SINGLE_CHOICE_ONLY = "error.poll.singleChoiceOnly";
    public static final String POLL_UNKNOWN_OPTION = "error.poll.unknownOption";
    public static final String POLL_ALREADY_ACTIVE = "error.poll.alreadyActive";
    public static final String POLL_ALREADY_CLOSED = "error.poll.alreadyClosed";
    public static final String POLL_INVALID_WINDOW = "error.poll.invalidWindow";

    // ---------------------------------------------------------------- statistika

    public static final String STATS_NO_FACULTY = "stats.noFaculty";

    // ---------------------------------------------------------------- email

    public static final String EMAIL_RECEIVED_SUBJECT = "email.complaintReceived.subject";
    public static final String EMAIL_RECEIVED_BODY = "email.complaintReceived.body";
    public static final String EMAIL_STATUS_SUBJECT = "email.statusChanged.subject";
    public static final String EMAIL_STATUS_BODY = "email.statusChanged.body";
    public static final String EMAIL_STATUS_RESPONSE_BLOCK = "email.statusChanged.responseBlock";
    public static final String EMAIL_HISTORY_INITIAL = "message.complaint.historyInitial";
}
