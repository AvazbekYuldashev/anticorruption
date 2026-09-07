import { describe, expect, it } from 'vitest';
import { applyLink, applyMark, RICH_TEXT_MARKS, safeHref } from './richText';

describe('formatlash belgilari', () => {
  it('tanlangan matnni o\'raydi va o\'sha matnni tanlangan qoldiradi', () => {
    const result = applyMark('salom dunyo', 6, 11, RICH_TEXT_MARKS.bold, 'namuna');

    expect(result.text).toBe('salom **dunyo**');
    expect(result.text.slice(result.selection[0], result.selection[1])).toBe('dunyo');
  });

  it('hech narsa tanlanmagan bo\'lsa namuna qo\'yiladi va tanlanadi', () => {
    const result = applyMark('', 0, 0, RICH_TEXT_MARKS.italic, 'namuna');

    expect(result.text).toBe('*namuna*');
    expect(result.text.slice(result.selection[0], result.selection[1])).toBe('namuna');
  });

  it('matn o\'rtasiga qo\'yilganda atrofdagi matn saqlanadi', () => {
    const result = applyMark('bir ikki uch', 4, 8, RICH_TEXT_MARKS.underline, 'namuna');
    expect(result.text).toBe('bir __ikki__ uch');
  });

  it('havolada kursor manzil o\'rniga qo\'yiladi', () => {
    const result = applyLink('bosing', 0, 6, 'namuna', 'https://');

    expect(result.text).toBe('[bosing](https://)');
    expect(result.text.slice(result.selection[0], result.selection[1])).toBe('https://');
  });
});

describe('havola manzili', () => {
  it('oddiy sxemalar o\'tadi', () => {
    expect(safeHref('https://example.uz')).toContain('https://example.uz');
    expect(safeHref('http://example.uz')).toContain('http://example.uz');
    expect(safeHref('mailto:xodim@example.uz')).toContain('mailto:');
  });

  it('kod bajaradigan manzillar havola bo\'lmaydi', () => {
    expect(safeHref('javascript:alert(1)')).toBeNull();
    expect(safeHref('JavaScript:alert(1)')).toBeNull();
    expect(safeHref('data:text/html,<script>alert(1)</script>')).toBeNull();
    expect(safeHref('vbscript:msgbox(1)')).toBeNull();
  });
});
