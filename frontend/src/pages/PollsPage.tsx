import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pollsApi, type PollAnswer } from '../api/polls';
import type {
  PollQuestionResponse,
  PollResponse,
  PollType,
  QuizResultResponse,
} from '../api/types';
import {
  Button,
  Card,
  EmptyState,
  ErrorBox,
  PageHeader,
  Spinner,
  StatusBadge,
} from '../components/ui';
import { errorMessage } from '../lib/errors';
import { formatDate } from '../lib/format';

/** Savol id si -> tanlangan variant id lari. */
type Selection = Record<number, number[]>;

/**
 * Bitta savol bloki: matni, tartib raqami va variantlari.
 *
 * <p>Javob berish paytida ham, natijalarni ko'rsatishda ham shu blok
 * ishlatiladi - ikki joyda ikki xil ko'rinish bo'lib qolmasligi uchun.
 */
function QuestionBlock({
  pollId,
  question,
  index,
  chosen,
  showResults,
  quiz,
  onToggle,
}: {
  pollId: number;
  question: PollQuestionResponse;
  index: number;
  chosen: number[];
  showResults: boolean;
  quiz: boolean;
  onToggle: (question: PollQuestionResponse, optionId: number) => void;
}) {
  const { t } = useTranslation();

  return (
    <div className="rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-800/40 p-4 sm:p-5">
      <p className="flex gap-2.5 text-sm font-semibold text-slate-900 dark:text-slate-100">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white">
          {index + 1}
        </span>
        <span>
          {question.text}
          {!question.required && (
            <span className="ml-1.5 text-xs font-normal text-slate-400 dark:text-slate-500">{t('polls.optional')}</span>
          )}
        </span>
      </p>

      {question.multipleChoice && !showResults && (
        <p className="mt-1.5 ml-8.5 text-xs text-slate-500 dark:text-slate-400">{t('polls.multipleHint')}</p>
      )}

      <div className="mt-3 space-y-2.5">
        {question.options.map((option) => {
          if (!showResults) {
            return (
              <label
                key={option.id}
                className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-3 py-2.5 transition-colors hover:border-brand-300 dark:hover:border-brand-400/40 hover:bg-brand-50/50 dark:hover:bg-brand-500/10"
              >
                <input
                  type={question.multipleChoice ? 'checkbox' : 'radio'}
                  name={`poll-${pollId}-question-${question.id}`}
                  checked={chosen.includes(option.id)}
                  onChange={() => onToggle(question, option.id)}
                  className="h-4 w-4 text-brand-600 dark:text-brand-300 focus:ring-brand-500"
                />
                <span className="text-sm text-slate-700 dark:text-slate-300">{option.text}</span>
              </label>
            );
          }

          // Testda foizlar emas, to'g'ri javob muhim.
          if (quiz && option.correct !== null) {
            const picked = chosen.includes(option.id);
            const wrong = picked && !option.correct;

            return (
              <div
                key={option.id}
                className={`flex items-center gap-3 rounded-lg border px-3 py-2.5 ${
                  option.correct
                    ? 'border-emerald-300 dark:border-emerald-500/40 bg-emerald-50 dark:bg-emerald-500/15'
                    : wrong
                      ? 'border-rose-300 dark:border-rose-500/40 bg-rose-50 dark:bg-rose-500/15'
                      : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900'
                }`}
              >
                <span
                  aria-hidden
                  className={`text-sm ${
                    option.correct ? 'text-emerald-600 dark:text-emerald-400' : wrong ? 'text-rose-600 dark:text-rose-400' : 'text-slate-300 dark:text-slate-600'
                  }`}
                >
                  {option.correct ? '✓' : wrong ? '✗' : '•'}
                </span>
                <span className="text-sm text-slate-700 dark:text-slate-300">{option.text}</span>
                {picked && (
                  <span className="ml-auto shrink-0 text-xs text-slate-500 dark:text-slate-400">
                    {t('tests.yourAnswer')}
                  </span>
                )}
              </div>
            );
          }

          return (
            <div key={option.id}>
              <div className="flex items-baseline justify-between gap-3 text-sm">
                <span className="text-slate-700 dark:text-slate-300">{option.text}</span>
                <span className="shrink-0 font-medium text-slate-900 dark:text-slate-100">{option.percentage}%</span>
              </div>
              <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                <div
                  className="h-full rounded-full bg-brand-500 transition-all"
                  style={{ width: `${option.percentage}%` }}
                />
              </div>
              <p className="mt-0.5 text-xs text-slate-400 dark:text-slate-500">
                {t('polls.votes', { count: option.voteCount })}
              </p>
            </div>
          );
        })}
      </div>

      {showResults && !quiz && (
        <p className="mt-2 text-xs text-slate-400 dark:text-slate-500">
          {t('polls.answered', { count: question.answeredCount })}
        </p>
      )}
    </div>
  );
}

/**
 * Bitta so'rovnoma yoki test.
 *
 * <p>Savollar birin-ketin beriladi: bir vaqtda bittasi ko'rinadi, javob
 * berilgach keyingisiga o'tiladi va istalgan paytda orqaga qaytish mumkin.
 * Uzun anketa shu tariqa qo'rqinchli ko'rinmaydi va qaysi savolda turgani
 * har doim aniq bo'ladi.
 *
 * <p>Javob yuborilgandan keyin yoki muddat tugaganda barcha savollar
 * natijalari bilan birga ko'rsatiladi.
 */
export function PollCard({ poll }: { poll: PollResponse }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<Selection>({});
  const [step, setStep] = useState(0);

  /*
   * Savollari tasodifiy beriladigan testda savollar sahifa bilan kelmaydi:
   * ishtirokchi "Testni boshlash"ni bosganda server unga to'plam tanlaydi.
   * Qayta bosilsa ham o'sha to'plam qaytadi.
   */
  const start = useMutation({
    mutationFn: () => pollsApi.start(poll.id),
    onSuccess: () => {
      setSelection({});
      setStep(0);
    },
  });

  const mutation = useMutation({
    mutationFn: () => pollsApi.vote(poll.id, toAnswers(selection), start.data?.attemptToken),
    onSuccess: () => {
      // Ro'yxatdagi va bosh sahifadagi nusxalari ham yangilansin.
      void queryClient.invalidateQueries({ queryKey: ['polls'] });
    },
  });

  /*
   * Javob yuborilgandan keyin serverning javobi ishlatiladi: unda test
   * natijasi va to'g'ri variantlar bo'ladi - ro'yxatdagi nusxada ular
   * hali berkitilgan. Test boshlangach esa ishtirokchiga tushgan savollar.
   */
  const view = mutation.data ?? start.data ?? poll;
  const quizResult: QuizResultResponse | null = mutation.data?.quizResult ?? null;
  const quiz = view.type === 'QUIZ';
  const showResults = view.alreadyVoted || !view.openForVoting || mutation.isSuccess;
  const needsStart = poll.questionsPerAttempt !== null && !start.data && !showResults;

  function toggle(question: PollQuestionResponse, optionId: number) {
    setSelection((current) => {
      const chosen = current[question.id] ?? [];

      if (!question.multipleChoice) {
        return { ...current, [question.id]: [optionId] };
      }
      return {
        ...current,
        [question.id]: chosen.includes(optionId)
          ? chosen.filter((id) => id !== optionId)
          : [...chosen, optionId],
      };
    });
  }

  const total = view.questions.length;
  const current = view.questions[Math.min(step, Math.max(total - 1, 0))];
  const currentChosen = current ? (selection[current.id] ?? []) : [];
  const answeredCount = view.questions.filter(
    (question) => (selection[question.id] ?? []).length > 0,
  ).length;
  const isLast = step >= total - 1;

  // Majburiy savollarning hammasi javoblanmaguncha yuborish mumkin emas -
  // server ham shuni talab qiladi, lekin xatoni kutib turishdan ko'ra
  // tugmani o'chirib qo'ygan tushunarliroq.
  const missingRequired = view.questions.some(
    (question) => question.required && (selection[question.id] ?? []).length === 0,
  );

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <h3 className="font-medium text-slate-900 dark:text-slate-100">{view.title}</h3>
        <StatusBadge status={view.status} label={view.statusLabel} />
      </div>

      {view.description && <p className="mt-1.5 text-sm text-slate-600 dark:text-slate-400">{view.description}</p>}

      {(view.startsAt || view.endsAt) && (
        <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
          {t('polls.period', {
            from: formatDate(view.startsAt),
            to: formatDate(view.endsAt),
          })}
        </p>
      )}

      {quizResult && (
        <div className="mt-4 rounded-xl border border-brand-200 dark:border-brand-400/30 bg-brand-50 dark:bg-brand-500/15 p-4">
          <p className="text-sm font-medium text-brand-800 dark:text-brand-200">
            {t('tests.resultTitle', {
              correct: quizResult.correctCount,
              total: quizResult.questionCount,
            })}
          </p>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-white dark:bg-slate-900">
            <div
              className="h-full rounded-full bg-brand-500 transition-all"
              style={{ width: `${quizResult.percentage}%` }}
            />
          </div>
          <p className="mt-1 text-xs text-brand-700 dark:text-brand-300">
            {t('tests.resultPercentage', { percentage: quizResult.percentage })}
          </p>
        </div>
      )}

      {showResults ? (
        <div className="mt-6 space-y-5">
          {view.questions.map((question, index) => (
            <QuestionBlock
              key={question.id}
              pollId={view.id}
              question={question}
              index={index}
              chosen={selection[question.id] ?? []}
              showResults
              quiz={quiz}
              onToggle={toggle}
            />
          ))}
        </div>
      ) : needsStart ? (
        <RandomQuizIntro
          count={poll.questionsPerAttempt ?? 0}
          total={poll.questionCount}
          busy={start.isPending}
          error={start.isError ? errorMessage(start.error, t) : null}
          onStart={() => start.mutate()}
        />
      ) : (
        current && (
          <>
            {/* Qaysi savolda turgani va qanchasi qolgani doim ko'rinib tursin. */}
            {total > 1 && (
              <div className="mt-5">
                <div className="flex flex-wrap items-baseline justify-between gap-2 text-xs">
                  <span className="font-medium text-slate-700 dark:text-slate-300">
                    {t('polls.step', { current: step + 1, total })}
                  </span>
                  <span className="text-slate-500 dark:text-slate-400">
                    {t('polls.progress', { done: answeredCount, left: total - answeredCount })}
                  </span>
                </div>
                <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                  <div
                    className="h-full rounded-full bg-brand-500 transition-all"
                    style={{ width: `${((step + 1) / total) * 100}%` }}
                  />
                </div>
              </div>
            )}

            <div className="mt-4">
              <QuestionBlock
                pollId={view.id}
                question={current}
                index={step}
                chosen={currentChosen}
                showResults={false}
                quiz={quiz}
                onToggle={toggle}
              />
            </div>

            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
              <Button
                variant="outline"
                disabled={step === 0}
                onClick={() => setStep((value) => Math.max(0, value - 1))}
              >
                ← {t('common.back')}
              </Button>

              {isLast ? (
                <Button
                  onClick={() => mutation.mutate()}
                  disabled={missingRequired || mutation.isPending}
                >
                  {mutation.isPending
                    ? t(quiz ? 'tests.submitting' : 'polls.voting')
                    : t(quiz ? 'tests.submit' : 'polls.vote')}
                </Button>
              ) : (
                <Button
                  // Majburiy savolni tashlab ketib bo'lmaydi.
                  disabled={current.required && currentChosen.length === 0}
                  onClick={() => setStep((value) => Math.min(total - 1, value + 1))}
                >
                  {t('common.next')} →
                </Button>
              )}
            </div>
          </>
        )
      )}

      <div className="mt-5 flex flex-wrap items-center gap-3">
        {/*
          Testning umumiy raqamlari ochiq saytga chiqmaydi: ishtirokchi
          faqat o'z natijasini ko'radi, hisobot esa admin panelida.
        */}
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {view.alreadyVoted || mutation.isSuccess
            ? t(quiz ? 'tests.done' : 'polls.voted')
            : !view.openForVoting
              ? t(quiz ? 'tests.closed' : 'polls.closed')
              : quiz
                ? t('tests.resultsHint')
                : t('polls.voters', { count: view.voterCount })}
        </span>
      </div>

      {mutation.isError && (
        <div className="mt-3">
          <ErrorBox message={errorMessage(mutation.error, t)} />
        </div>
      )}
    </Card>
  );
}

/**
 * Savollari tasodifiy beriladigan testning boshlanish oynasi.
 *
 * <p>To'plam sahifa ochilishi bilan emas, tugma bosilganda tanlanadi: ishtirokchi
 * oldin nechta savol olishini ko'radi, testga ko'z tashlab chiqib ketgan
 * tashrifchiga esa savollar ajratilmaydi.
 */
function RandomQuizIntro({
  count,
  total,
  busy,
  error,
  onStart,
}: {
  count: number;
  total: number;
  busy: boolean;
  error: string | null;
  onStart: () => void;
}) {
  const { t } = useTranslation();

  return (
    <div className="mt-5 rounded-2xl border border-brand-200 dark:border-brand-400/30 bg-gradient-to-br from-brand-50 dark:from-brand-500/10 via-white dark:via-slate-900 to-accent-300/10 p-5 shadow-sm sm:p-6">
      <div className="flex items-start gap-4">
        <span
          aria-hidden
          className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-md shadow-brand-700/25"
        >
          {/* Aralashtirish belgisi */}
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.7-1.1 2-1.7 3.3-1.7H22" />
            <path d="m18 2 4 4-4 4" />
            <path d="M2 6h1.9c1.5 0 2.9.9 3.6 2.2" />
            <path d="M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8" />
            <path d="m18 14 4 4-4 4" />
          </svg>
        </span>

        <div className="min-w-0">
          <p className="text-base font-semibold text-slate-900 dark:text-slate-100">
            {t('polls.questionCount', { count })}
          </p>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{t('tests.randomIntro', { count, total })}</p>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{t('tests.randomNote')}</p>
        </div>
      </div>

      <div className="mt-5">
        <Button onClick={onStart} disabled={busy}>
          {busy ? t('tests.starting') : t('tests.start')}
          {!busy && <span aria-hidden>→</span>}
        </Button>
      </div>

      {error && (
        <div className="mt-3">
          <ErrorBox message={error} />
        </div>
      )}
    </div>
  );
}

/** Javobsiz qolgan savollar yuborilmaydi - server ularni javobsiz deb hisoblaydi. */
function toAnswers(selection: Selection): PollAnswer[] {
  return Object.entries(selection)
    .filter(([, optionIds]) => optionIds.length > 0)
    .map(([questionId, optionIds]) => ({ questionId: Number(questionId), optionIds }));
}

/**
 * Ro'yxatdagi bitta yozuv.
 *
 * <p>Savollar bu yerda ochilmaydi: ro'yxat qisqa va o'qishga qulay bo'lishi
 * kerak. Tashrifchi kerakli so'rovnoma yoki testni tanlaydi va uning
 * sahifasida ishtirok etadi.
 */
export function PollListItem({ poll }: { poll: PollResponse }) {
  const { t } = useTranslation();
  const quiz = poll.type === 'QUIZ';
  const done = poll.alreadyVoted || !poll.openForVoting;

  return (
    <Link
      to={`${quiz ? '/tests' : '/polls'}/${poll.id}`}
      className="group flex flex-col gap-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 shadow-sm transition-all hover:border-brand-300 dark:hover:border-brand-400/40 hover:shadow-md sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="font-semibold text-slate-900 dark:text-slate-100 group-hover:text-brand-700 dark:group-hover:text-brand-300">{poll.title}</h2>
          <StatusBadge status={poll.status} label={poll.statusLabel} />
        </div>

        {poll.description && (
          <p className="mt-1.5 line-clamp-2 text-sm text-slate-600 dark:text-slate-400">{poll.description}</p>
        )}

        <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
          <span>
            {poll.questionsPerAttempt !== null
              ? t('tests.randomCount', { count: poll.questionsPerAttempt, total: poll.questionCount })
              : t('polls.questionCount', { count: poll.questionCount })}
          </span>
          {/* Testda ishtirokchilar soni ochiq saytda ko'rsatilmaydi. */}
          {!quiz && (
            <>
              <span aria-hidden>•</span>
              <span>{t('polls.voters', { count: poll.voterCount })}</span>
            </>
          )}
          {(poll.startsAt || poll.endsAt) && (
            <>
              <span aria-hidden>•</span>
              <span>
                {t('polls.period', {
                  from: formatDate(poll.startsAt),
                  to: formatDate(poll.endsAt),
                })}
              </span>
            </>
          )}
        </div>
      </div>

      <span className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white transition-colors group-hover:bg-brand-700">
        {done ? t('polls.viewResults') : t(quiz ? 'tests.start' : 'polls.participate')}
        <span aria-hidden>→</span>
      </span>
    </Link>
  );
}

/** So'rovnomalar va testlar bir xil ko'rinishda chiqadi, faqat turi boshqa. */
function PollListPage({ type }: { type: PollType }) {
  const { t } = useTranslation();
  const query = useQuery({
    queryKey: ['polls', 'active', type],
    queryFn: () => pollsApi.active(type),
  });

  const quiz = type === 'QUIZ';

  if (query.isPending) return <Spinner />;
  if (query.isError)
    return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title={t(quiz ? 'tests.title' : 'polls.title')}
        description={t(quiz ? 'tests.intro' : 'polls.intro')}
      />

      {query.data.length === 0 ? (
        <EmptyState message={t(quiz ? 'tests.empty' : 'polls.empty')} />
      ) : (
        <div className="space-y-4">
          {query.data.map((poll) => (
            <PollListItem key={poll.id} poll={poll} />
          ))}
        </div>
      )}
    </div>
  );
}

/** Bitta so'rovnoma yoki test sahifasi - shu yerda ishtirok etiladi. */
function PollDetailPage({ type }: { type: PollType }) {
  const { t } = useTranslation();
  const { id = '' } = useParams();
  const quiz = type === 'QUIZ';

  const query = useQuery({
    queryKey: ['polls', 'detail', id],
    queryFn: () => pollsApi.detail(Number(id)),
    retry: false,
  });

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to={quiz ? '/tests' : '/polls'}
        className="text-sm font-medium text-brand-600 dark:text-brand-300 hover:underline"
      >
        ← {t(quiz ? 'tests.backToList' : 'polls.backToList')}
      </Link>

      <div className="mt-4">
        {query.isPending && <Spinner />}
        {query.isError && <ErrorBox message={errorMessage(query.error, t)} />}
        {query.data && <PollCard poll={query.data} />}
      </div>
    </div>
  );
}

export function PollsPage() {
  return <PollListPage type="SURVEY" />;
}

export function QuizzesPage() {
  return <PollListPage type="QUIZ" />;
}

export function PollDetailsPage() {
  return <PollDetailPage type="SURVEY" />;
}

export function QuizDetailsPage() {
  return <PollDetailPage type="QUIZ" />;
}
