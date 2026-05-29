import { describe, expect, it } from 'vitest';
import { getActivityImage } from '../views/visitor/ActivityBooking.vue';

describe('activity card imagery', () => {
  it('matches zoo activities with contextual real-photo assets', () => {
    const giraffe = getActivityImage({ title: '长颈鹿科普讲解', category: '科普讲解' });
    const classroom = getActivityImage({ title: '小小饲养员亲子课堂', category: '亲子课堂' });
    const night = getActivityImage({ title: '夏夜动物园', category: '夜游活动' });
    const rainforest = getActivityImage({ title: '雨林探秘导览', category: '主题导览' });

    expect(giraffe.alt).toContain('长颈鹿');
    expect(classroom.alt).toContain('亲子');
    expect(night.alt).toContain('夜游');
    expect(rainforest.alt).toContain('雨林');
    expect([giraffe, classroom, night, rainforest].every((image) => image.src.startsWith('https://commons.wikimedia.org/'))).toBe(true);
  });
});
