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
    public static final String ERROR_TOO_MANY_REQUESTS = "error.tooManyRequests";
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
    public static final String ERROR_AUTH_REFRESH_INVALID = "error.auth.refreshInvalid";
    public static final String ERROR_CSRF_INVALID = "error.csrf.invalid";

    // ---------------------------------------------------------------- topilmadi

    public static final String NOT_FOUND_COMPLAINT = "error.notFound.complaint";
    public static final String NOT_FOUND_USER = "error.notFound.user";
    public static final String NOT_FOUND_STAFF_USER = "error.notFound.staffUser";
    public static final String NOT_FOUND_FACULTY = "error.notFound.faculty";
    public static final String NOT_FOUND_DEPARTMENT = "error.notFound.department";
    public static final String NOT_FOUND_ATTACHMENT = "error.notFound.attachment";
    public static final String NOT_FOUND_NEWS = "error.notFound.news";
    public static final String NOT_FOUND_STAFF_MEMBER = "error.notFound.staffMember";
    public static final String NOT_FOUND_POLL = "error.notFound.poll";
    public static final String NOT_FOUND_POLL_GROUP = "error.notFound.pollGroup";
    public static final String NOT_FOUND_NEWS_IMAGE = "error.notFound.newsImage";
    public static final String NOT_FOUND_HOME_BANNER_IMAGE = "error.notFound.homeBannerImage";

    // ---------------------------------------------------------------- murojaat

    public static final String COMPLAINT_NOT_FOUND_BY_CODE = "error.complaint.notFoundByCode";
    public static final String COMPLAINT_ALREADY_IN_STATUS = "error.complaint.alreadyInStatus";
    public static final String COMPLAINT_TRANSITION_NOT_ALLOWED = "error.complaint.transitionNotAllowed";
    public static final String COMPLAINT_ASSIGNEE_MUST_BE_STAFF = "error.complaint.assigneeMustBeStaff";
    public static final String COMPLAINT_ASSIGNEE_DISABLED = "error.complaint.assigneeDisabled";
    public static final String COMPLAINT_ALREADY_HIDDEN = "error.complaint.alreadyHidden";
    public static final String COMPLAINT_ALREADY_VISIBLE = "error.complaint.alreadyVisible";
    public static final String COMPLAINT_ACCEPTED = "message.complaint.accepted";
    public static final String COMPLAINT_RESPONSE_EMPTY = "error.complaint.responseEmpty";
    public static final String COMPLAINT_RESPONSE_HISTORY = "message.complaint.responseHistory";

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
    public static final String USER_CANNOT_DELETE_SELF = "error.user.cannotDeleteSelf";
    public static final String USER_HAS_COMPLAINTS = "error.user.hasComplaints";
    public static final String USER_CURRENT_PASSWORD_WRONG = "error.user.currentPasswordWrong";
    public static final String USER_PASSWORD_NOT_CHANGED = "error.user.passwordNotChanged";
    public static final String USER_OWN_PASSWORD_IN_PROFILE = "error.user.ownPasswordInProfile";

    // ---------------------------------------------------------------- fayllar

    public static final String FILE_EMPTY = "error.file.empty";
    public static final String FILE_NONE_SELECTED = "error.file.noneSelected";
    public static final String FILE_TYPE_NOT_ALLOWED = "error.file.typeNotAllowed";
    public static final String FILE_IMAGE_TYPE_NOT_ALLOWED = "error.file.imageTypeNotAllowed";
    public static final String FILE_INVALID_NAME = "error.file.invalidName";
    public static final String FILE_CONTENT_MISMATCH = "error.file.contentMismatch";
    public static final String FILE_NOT_FOUND = "error.file.notFound";
    public static final String FILE_UNREADABLE = "error.file.unreadable";
    public static final String FILE_TOO_LARGE = "error.file.tooLarge";
    public static final String FILE_INFECTED = "error.file.infected";
    public static final String FILE_SCAN_UNAVAILABLE = "error.file.scanUnavailable";
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
    public static final String NEWS_TRANSLATION_EXISTS = "error.news.translationExists";
    public static final String SITE_TEXT_UNKNOWN_KEY = "error.siteText.unknownKey";
    public static final String SITE_TEXT_UNKNOWN_LANGUAGE = "error.siteText.unknownLanguage";
    public static final String HOME_BANNER_TOO_MANY = "error.homeBanner.tooMany";
    public static final String HOME_BANNER_ORDER_MISMATCH = "error.homeBanner.orderMismatch";

    // ---------------------------------------------------------------- so'rovnoma

    public static final String POLL_CLOSED = "error.poll.closed";
    public static final String POLL_ALREADY_VOTED = "error.poll.alreadyVoted";
    public static final String POLL_SINGLE_CHOICE_ONLY = "error.poll.singleChoiceOnly";
    public static final String POLL_UNKNOWN_OPTION = "error.poll.unknownOption";
    public static final String POLL_UNKNOWN_QUESTION = "error.poll.unknownQuestion";
    public static final String POLL_DUPLICATE_ANSWER = "error.poll.duplicateAnswer";
    public static final String POLL_QUESTION_REQUIRED = "error.poll.questionRequired";
    public static final String POLL_ALREADY_ACTIVE = "error.poll.alreadyActive";
    public static final String POLL_ALREADY_CLOSED = "error.poll.alreadyClosed";
    public static final String POLL_INVALID_WINDOW = "error.poll.invalidWindow";
    public static final String POLL_ALREADY_STOPPED = "error.poll.alreadyStopped";
    public static final String POLL_NOT_STOPPED = "error.poll.notStopped";
    public static final String POLL_QUIZ_NO_CORRECT = "error.poll.quizNoCorrect";
    public static final String POLL_QUIZ_SINGLE_CORRECT = "error.poll.quizSingleCorrect";
    public static final String POLL_DUPLICATE_QUESTION = "error.poll.duplicateQuestion";
    public static final String POLL_ATTEMPT_NOT_FOUND = "error.poll.attemptNotFound";
    public static final String POLL_GROUP_NAME_TAKEN = "error.pollGroup.nameTaken";
    public static final String POLL_GROUP_TYPE_MISMATCH = "error.pollGroup.typeMismatch";
    public static final String POLL_GROUP_REQUIRED = "error.pollGroup.required";
    public static final String POLL_GROUP_IN_USE = "error.pollGroup.inUse";

    // ---------------------------------------------------------------- statistika

    public static final String STATS_NO_FACULTY = "stats.noFaculty";

    // ---------------------------------------------------------------- email

    public static final String EMAIL_RECEIVED_SUBJECT = "email.complaintReceived.subject";
    public static final String EMAIL_RECEIVED_BODY = "email.complaintReceived.body";
    public static final String EMAIL_STATUS_SUBJECT = "email.statusChanged.subject";
    public static final String EMAIL_STATUS_BODY = "email.statusChanged.body";
    public static final String EMAIL_STATUS_RESPONSE_BLOCK = "email.statusChanged.responseBlock";
    public static final String EMAIL_RESPONSE_SUBJECT = "email.responseAdded.subject";
    public static final String EMAIL_RESPONSE_BODY = "email.responseAdded.body";
    public static final String EMAIL_HISTORY_INITIAL = "message.complaint.historyInitial";
}
