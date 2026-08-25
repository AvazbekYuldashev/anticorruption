import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  FormControlLabel,
  IconButton,
  Paper,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import type { SavePollQuestion } from '../api/polls';
import type { PollQuestionResponse } from '../api/types';

/**
 * Muharrirdagi variant.
 *
 * <p>`id` - serverdagi variant. U saqlanib qolsa ovozlari ham qoladi;
 * `null` bo'lsa server uni yangi variant deb yozadi.
 */
export interface EditorOption {
  key: string;
  id: number | null;
  text: string;
}

export interface EditorQuestion {
  key: string;
  id: number | null;
  text: string;
  multipleChoice: boolean;
  required: boolean;
  options: EditorOption[];
}

let keyCounter = 0;

function newKey(): string {
  keyCounter += 1;
  return `poll-${keyCounter}`;
}

export function emptyOption(): EditorOption {
  return { key: newKey(), id: null, text: '' };
}

export function emptyQuestion(): EditorQuestion {
  return {
    key: newKey(),
    id: null,
    text: '',
    multipleChoice: false,
    required: true,
    options: [emptyOption(), emptyOption()],
  };
}

export function toEditorQuestions(questions: PollQuestionResponse[]): EditorQuestion[] {
  return questions.map((question) => ({
    key: newKey(),
    id: question.id,
    text: question.text,
    multipleChoice: question.multipleChoice,
    required: question.required,
    options: question.options.map((option) => ({
      key: newKey(),
      id: option.id,
      text: option.text,
    })),
  }));
}

/** Bo'sh variantlar va bo'sh savollar yuborilmaydi. */
export function toSaveQuestions(questions: EditorQuestion[]): SavePollQuestion[] {
  return questions
    .filter((question) => question.text.trim() !== '')
    .map((question) => ({
      id: question.id,
      text: question.text,
      multipleChoice: question.multipleChoice,
      required: question.required,
      options: question.options
        .filter((option) => option.text.trim() !== '')
        .map((option) => ({ id: option.id, text: option.text })),
    }));
}

interface Props {
  value: EditorQuestion[];
  onChange: (questions: EditorQuestion[]) => void;
}

/**
 * So'rovnoma savollarini tahrirlaydi.
 *
 * <p>Savollar soni cheklanmagan, har birida kamida ikkita variant bo'lishi
 * kerak - shuning uchun oxirgi ikkitasini o'chirish tugmasi o'chiriladi.
 */
export function PollQuestionsEditor({ value, onChange }: Props) {
  const { t } = useTranslation();

  function updateQuestion(index: number, patch: Partial<EditorQuestion>) {
    onChange(value.map((question, i) => (i === index ? { ...question, ...patch } : question)));
  }

  function moveQuestion(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= value.length) return;
    const next = [...value];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  function updateOption(questionIndex: number, optionIndex: number, text: string) {
    const options = value[questionIndex].options.map((option, i) =>
      i === optionIndex ? { ...option, text } : option,
    );
    updateQuestion(questionIndex, { options });
  }

  return (
    <Box>
      <Stack spacing={2}>
        {value.map((question, index) => (
          <Paper key={question.key} variant="outlined" sx={{ p: 2 }}>
            <Stack
              direction="row"
              spacing={1}
              sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}
            >
              <Typography variant="caption" color="text.secondary">
                {t('admin.questionNumber', { number: index + 1 })}
              </Typography>

              <Stack direction="row" spacing={0.5}>
                <IconButton
                  size="small"
                  disabled={index === 0}
                  onClick={() => moveQuestion(index, -1)}
                  aria-label={t('admin.moveUp')}
                >
                  <Typography variant="caption">↑</Typography>
                </IconButton>
                <IconButton
                  size="small"
                  disabled={index === value.length - 1}
                  onClick={() => moveQuestion(index, 1)}
                  aria-label={t('admin.moveDown')}
                >
                  <Typography variant="caption">↓</Typography>
                </IconButton>
                <IconButton
                  size="small"
                  color="error"
                  disabled={value.length <= 1}
                  onClick={() => onChange(value.filter((_, i) => i !== index))}
                  aria-label={t('admin.removeQuestion')}
                >
                  <Typography variant="caption">×</Typography>
                </IconButton>
              </Stack>
            </Stack>

            <TextField
              label={t('admin.fieldQuestion')}
              value={question.text}
              onChange={(event) => updateQuestion(index, { text: event.target.value })}
              required
              fullWidth
              size="small"
            />

            <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
              {t('admin.fieldOptions')}
            </Typography>

            <Stack spacing={1.5}>
              {question.options.map((option, optionIndex) => (
                <Stack
                  key={option.key}
                  direction="row"
                  spacing={1}
                  sx={{ alignItems: 'center' }}
                >
                  <TextField
                    size="small"
                    value={option.text}
                    onChange={(event) => updateOption(index, optionIndex, event.target.value)}
                    fullWidth
                  />
                  <IconButton
                    size="small"
                    color="error"
                    disabled={question.options.length <= 2}
                    onClick={() =>
                      updateQuestion(index, {
                        options: question.options.filter((_, i) => i !== optionIndex),
                      })
                    }
                    aria-label={t('admin.removeOption')}
                  >
                    <Typography variant="caption">×</Typography>
                  </IconButton>
                </Stack>
              ))}
            </Stack>

            <Button
              size="small"
              sx={{ mt: 1.5 }}
              disabled={question.options.length >= 50}
              onClick={() =>
                updateQuestion(index, { options: [...question.options, emptyOption()] })
              }
            >
              + {t('admin.addOption')}
            </Button>

            <Stack direction="row" spacing={3} sx={{ flexWrap: 'wrap', mt: 1 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={question.multipleChoice}
                    onChange={(event) =>
                      updateQuestion(index, { multipleChoice: event.target.checked })
                    }
                  />
                }
                label={t('admin.multipleChoice')}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={question.required}
                    onChange={(event) => updateQuestion(index, { required: event.target.checked })}
                  />
                }
                label={t('admin.questionRequired')}
              />
            </Stack>
          </Paper>
        ))}
      </Stack>

      <Button
        variant="outlined"
        size="small"
        sx={{ mt: 2 }}
        onClick={() => onChange([...value, emptyQuestion()])}
      >
        + {t('admin.addQuestion')}
      </Button>
    </Box>
  );
}
