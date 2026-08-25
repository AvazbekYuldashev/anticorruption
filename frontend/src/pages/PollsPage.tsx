import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pollsApi, type PollAnswer } from '../api/polls';
import type { PollQuestionResponse, PollResponse } from '../api/types';
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
 * Bitta so'rovnoma.
 *
 * <p>Savollar soni cheklanmagan: hammasi bitta kartada chiqadi va bir
 * marta yuboriladi. Ovoz berilgandan keyin yoki muddat tugaganda o'rniga
 * natijalar ko'rsatiladi.
 */
export function PollCard({ poll }: { poll: PollResponse }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<Selection>({});

  const mutation = useMutation({
    mutationFn: () => pollsApi.vote(poll.id, toAnswers(selection)),
    onSuccess: () => {
      // Ro'yxatdagi va bosh sahifadagi nusxalari ham yangilansin.
      void queryClient.invalidateQueries({ queryKey: ['polls'] });
    },
  });

  const showResults = poll.alreadyVoted || !poll.openForVoting || mutation.isSuccess;

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

  // Majburiy savollarning hammasi javoblanmaguncha yuborish mumkin emas -
  // server ham shuni talab qiladi, lekin xatoni kutib turishdan ko'ra
  // tugmani o'chirib qo'ygan tushunarliroq.
  const missingRequired = poll.questions.some(
    (question) => question.required && (selection[question.id] ?? []).length === 0,
  );

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <h3 className="font-medium text-slate-900">{poll.title}</h3>
        <StatusBadge status={poll.status} label={poll.statusLabel} />
      </div>

      {poll.description && <p className="mt-1.5 text-sm text-slate-600">{poll.description}</p>}

      {(poll.startsAt || poll.endsAt) && (
        <p className="mt-1.5 text-xs text-slate-500">
          {t('polls.period', {
            from: formatDate(poll.startsAt),
            to: formatDate(poll.endsAt),
          })}
        </p>
      )}

      <div className="mt-5 space-y-6">
        {poll.questions.map((question, index) => (
          <div key={question.id}>
            <p className="text-sm font-medium text-slate-900">
              {poll.questions.length > 1 && <span className="text-slate-400">{index + 1}. </span>}
              {question.text}
              {!question.required && (
                <span className="ml-1.5 text-xs font-normal text-slate-400">
                  {t('polls.optional')}
                </span>
              )}
            </p>

            {question.multipleChoice && !showResults && (
              <p className="mt-1 text-xs text-slate-500">{t('polls.multipleHint')}</p>
            )}

            <div className="mt-3 space-y-3">
              {question.options.map((option) =>
                showResults ? (
                  <div key={option.id}>
                    <div className="flex items-baseline justify-between gap-3 text-sm">
                      <span className="text-slate-700">{option.text}</span>
                      <span className="shrink-0 font-medium text-slate-900">
                        {option.percentage}%
                      </span>
                    </div>
                    <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full bg-brand-500 transition-all"
                        style={{ width: `${option.percentage}%` }}
                      />
                    </div>
                    <p className="mt-0.5 text-xs text-slate-400">
                      {t('polls.votes', { count: option.voteCount })}
                    </p>
                  </div>
                ) : (
                  <label
                    key={option.id}
                    className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 px-3 py-2.5 hover:bg-slate-50"
                  >
                    <input
                      type={question.multipleChoice ? 'checkbox' : 'radio'}
                      name={`poll-${poll.id}-question-${question.id}`}
                      checked={(selection[question.id] ?? []).includes(option.id)}
                      onChange={() => toggle(question, option.id)}
                      className="h-4 w-4 text-brand-600 focus:ring-brand-500"
                    />
                    <span className="text-sm text-slate-700">{option.text}</span>
                  </label>
                ),
              )}
            </div>

            {showResults && (
              <p className="mt-2 text-xs text-slate-400">
                {t('polls.answered', { count: question.answeredCount })}
              </p>
            )}
          </div>
        ))}
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-3">
        {!showResults && (
          <Button onClick={() => mutation.mutate()} disabled={missingRequired || mutation.isPending}>
            {mutation.isPending ? t('polls.voting') : t('polls.vote')}
          </Button>
        )}

        <span className="text-xs text-slate-500">
          {poll.alreadyVoted || mutation.isSuccess
            ? t('polls.voted')
            : !poll.openForVoting
              ? t('polls.closed')
              : t('polls.voters', { count: poll.voterCount })}
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

/** Javobsiz qolgan savollar yuborilmaydi - server ularni javobsiz deb hisoblaydi. */
function toAnswers(selection: Selection): PollAnswer[] {
  return Object.entries(selection)
    .filter(([, optionIds]) => optionIds.length > 0)
    .map(([questionId, optionIds]) => ({ questionId: Number(questionId), optionIds }));
}

export function PollsPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['polls', 'active'], queryFn: pollsApi.active });

  if (query.isPending) return <Spinner />;
  if (query.isError)
    return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t('polls.title')} description={t('polls.intro')} />

      {query.data.length === 0 ? (
        <EmptyState message={t('polls.empty')} />
      ) : (
        <div className="space-y-6">
          {query.data.map((poll) => (
            <PollCard key={poll.id} poll={poll} />
          ))}
        </div>
      )}
    </div>
  );
}
