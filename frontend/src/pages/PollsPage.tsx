import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pollsApi } from '../api/polls';
import type { PollResponse } from '../api/types';
import { Button, Card, EmptyState, ErrorBox, PageHeader, Spinner } from '../components/ui';
import { errorMessage } from '../lib/errors';

/**
 * Bitta so'rovnoma.
 *
 * <p>Ovoz berilgandan keyin yoki ovoz berish yopilganda natijalar
 * ko'rsatiladi; aks holda variantlar tanlash uchun chiqadi.
 */
export function PollCard({ poll }: { poll: PollResponse }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<number[]>([]);

  const mutation = useMutation({
    mutationFn: () => pollsApi.vote(poll.id, selected),
    onSuccess: () => {
      // Ro'yxatdagi va bosh sahifadagi nusxalari ham yangilansin.
      void queryClient.invalidateQueries({ queryKey: ['polls'] });
    },
  });

  const showResults = poll.alreadyVoted || !poll.openForVoting || mutation.isSuccess;

  function toggle(optionId: number) {
    setSelected((current) => {
      if (poll.multipleChoice) {
        return current.includes(optionId)
          ? current.filter((id) => id !== optionId)
          : [...current, optionId];
      }
      return [optionId];
    });
  }

  return (
    <Card>
      <h3 className="font-medium text-slate-900">{poll.question}</h3>
      {poll.description && <p className="mt-1.5 text-sm text-slate-600">{poll.description}</p>}
      {poll.multipleChoice && !showResults && (
        <p className="mt-1.5 text-xs text-slate-500">{t('polls.multipleHint')}</p>
      )}

      <div className="mt-4 space-y-3">
        {poll.options.map((option) => {
          if (showResults) {
            return (
              <div key={option.id}>
                <div className="flex items-baseline justify-between gap-3 text-sm">
                  <span className="text-slate-700">{option.text}</span>
                  <span className="shrink-0 font-medium text-slate-900">{option.percentage}%</span>
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
            );
          }

          return (
            <label
              key={option.id}
              className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 px-3 py-2.5 hover:bg-slate-50"
            >
              <input
                type={poll.multipleChoice ? 'checkbox' : 'radio'}
                name={`poll-${poll.id}`}
                checked={selected.includes(option.id)}
                onChange={() => toggle(option.id)}
                className="h-4 w-4 text-brand-600 focus:ring-brand-500"
              />
              <span className="text-sm text-slate-700">{option.text}</span>
            </label>
          );
        })}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        {!showResults && (
          <Button
            onClick={() => mutation.mutate()}
            disabled={selected.length === 0 || mutation.isPending}
          >
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

export function PollsPage() {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ['polls', 'active'], queryFn: pollsApi.active });

  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorBox message={errorMessage(query.error, t)} onRetry={() => query.refetch()} />;

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
