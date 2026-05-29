import { describe, expect, it } from 'vitest';
import { filterGuidePois, getInitialPoi, guidePois } from '../views/visitor/GuideMap.vue';

describe('guide map data contract', () => {
  it('starts from a highlighted visitor entry point', () => {
    const initialPoi = getInitialPoi();

    expect(initialPoi.type).toBe('entrance');
    expect(initialPoi.status).toBe('open');
  });

  it('filters map points by type and popular mode', () => {
    const animalPois = filterGuidePois('animal', false);
    const popularPois = filterGuidePois('all', true);

    expect(animalPois.length).toBeGreaterThanOrEqual(2);
    expect(animalPois.every((poi) => poi.type === 'animal')).toBe(true);
    expect(popularPois.length).toBeGreaterThan(0);
    expect(popularPois.every((poi) => poi.tags.includes('热门'))).toBe(true);
  });

  it('keeps every point inside the svg map bounds', () => {
    expect(guidePois.length).toBeGreaterThanOrEqual(8);
    for (const poi of guidePois) {
      expect(poi.position.x).toBeGreaterThanOrEqual(0);
      expect(poi.position.x).toBeLessThanOrEqual(900);
      expect(poi.position.y).toBeGreaterThanOrEqual(0);
      expect(poi.position.y).toBeLessThanOrEqual(560);
    }
  });
});
