import { describe, expect, it } from 'vitest';
import { adminModules } from '../views/admin/adminModules';
import { buildInventoryPayload } from '../views/admin/AdminTablePage.vue';

describe('admin closure configuration', () => {
  it('uses explicit action keys instead of placeholder text buttons', () => {
    for (const config of Object.values(adminModules)) {
      for (const action of config.rowActions) {
        expect(action.key).toMatch(/^[a-z][a-zA-Z]+$/);
        expect(action.label).not.toContain('队列');
      }
    }
  });

  it('hides create actions for read/action-only business modules', () => {
    expect(adminModules.orders.canCreate).toBe(false);
    expect(adminModules.checkins.canCreate).toBe(false);
    expect(adminModules.activities.formFields.some((field) => field.key === 'startTime')).toBe(true);
  });

  it('builds inventory updates with date, session, and ticket code', () => {
    const payload = buildInventoryPayload({
      visitDate: '2026-06-02',
      session: 'PM',
      ticketTypeCode: 'ADULT',
      dailyCapacity: 600,
      dailyRemaining: 240,
      capacity: 300,
      remaining: 120,
    });

    expect(payload).toEqual({
      visitDate: '2026-06-02',
      session: 'PM',
      ticketTypeCode: 'ADULT',
      dailyCapacity: 600,
      dailyRemaining: 240,
      capacity: 300,
      remaining: 120,
    });
  });
});
