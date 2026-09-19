import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import '../i18n';
import { pollsApi } from '../api/polls';
import type { PollQuestionResponse, PollResponse } from '../api/types';
import { PollCard } from './PollsPage';

function question(id: number): PollQuestionResponse {
  return {
    id,
    text: `Savol matni ${id}`,
    multipleChoice: false,
    required: true,
    answeredCount: 0,
    displayOrder: id,
    options: [
      { id: id * 10 + 1, text: `Variant ${id}-A`, voteCount: 0, percentage: 0, correct: null },
      { id: id * 10 + 2, text: `Variant ${id}-B`, voteCount: 0, percentage: 0, correct: null },
    ],
  };
}

function quiz(overrides: Partial<PollResponse> = {}): PollResponse {
  return {
    id: 7,
    title: 'Korrupsiyaga qarshi test',
    description: null,
    type: 'QUIZ',
    typeLabel: 'Test',
    active: true,
    status: 'OPEN',
    statusLabel: 'Ochiq',
    openForVoting: true,
    alreadyVoted: false,
    startsAt: null,
    endsAt: null,
    stoppedAt: null,
    runNumber: 1,
    previousPollId: null,
    groupId: 1,
    groupName: 'Oylik 2026',
    voterCount: 0,
    questionCount: 200,
    questionsPerAttempt: 2,
    questions: [],
    quizResult: null,
    attemptToken: null,
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
    ...overrides,
  };
}

function renderCard(poll: PollResponse) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <PollCard poll={poll} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('PollCard - savollari tasodifiy beriladigan test', () => {
  afterEach(cleanup);

  it('boshlashdan oldin savollar yo\'q, faqat nechtasi berilishi ko\'rinadi', () => {
    const start = vi.spyOn(pollsApi, 'start');
    renderCard(quiz());

    expect(screen.getByText(/200 ta savoldan 2 tasi tasodifiy/)).toBeInTheDocument();
    expect(screen.queryByText(/Savol matni/)).not.toBeInTheDocument();
    // Tashrifchi tugmani bosmaguncha unga to'plam ajratilmaydi
    expect(start).not.toHaveBeenCalled();
  });

  it('tushgan savollarni ko\'rsatadi va javoblarni to\'plam belgisi bilan yuboradi', async () => {
    const user = userEvent.setup();
    const drawn = [question(42), question(5)];

    const start = vi.spyOn(pollsApi, 'start').mockResolvedValue(
      quiz({ questions: drawn, attemptToken: 'belgi-123' }),
    );
    const vote = vi.spyOn(pollsApi, 'vote').mockResolvedValue(
      quiz({ questions: drawn, alreadyVoted: true }),
    );

    renderCard(quiz());
    await user.click(screen.getByRole('button', { name: /Testni boshlash/ }));

    expect(start).toHaveBeenCalledWith(7);
    expect(await screen.findByText('Savol matni 42')).toBeInTheDocument();
    expect(screen.getByText('Savol 1 / 2')).toBeInTheDocument();

    await user.click(screen.getByLabelText('Variant 42-B'));
    await user.click(screen.getByRole('button', { name: /Keyingi/ }));
    await user.click(screen.getByLabelText('Variant 5-A'));
    await user.click(screen.getByRole('button', { name: /Javoblarni yuborish/ }));

    await waitFor(() => expect(vote).toHaveBeenCalledTimes(1));
    const [pollId, answers, attemptToken] = vote.mock.calls[0];
    expect(pollId).toBe(7);
    expect(attemptToken).toBe('belgi-123');
    // Tartib ahamiyatsiz: server javoblarni savol id si bo'yicha bog'laydi.
    expect(answers).toHaveLength(2);
    expect(answers).toEqual(
      expect.arrayContaining([
        { questionId: 42, optionIds: [422] },
        { questionId: 5, optionIds: [51] },
      ]),
    );
  });

  it('barcha savollari beriladigan testda savollar darhol ko\'rinadi', () => {
    const start = vi.spyOn(pollsApi, 'start');
    renderCard(quiz({ questionsPerAttempt: null, questionCount: 1, questions: [question(1)] }));

    expect(screen.getByText('Savol matni 1')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Testni boshlash/ })).not.toBeInTheDocument();
    expect(start).not.toHaveBeenCalled();
  });
});
