import { describe, expect, it } from 'vitest';
import { annualPassProducts, bookingTickets } from '../stores/booking';

describe('booking product grouping', () => {
  it('keeps regular ticket inventory separate from annual pass purchase products', () => {
    expect(bookingTickets.map((ticket) => ticket.code)).toEqual(['ADULT', 'CHILD', 'SENIOR']);
    expect(annualPassProducts.map((product) => product.code)).toEqual(['ANNUAL']);
  });
});
