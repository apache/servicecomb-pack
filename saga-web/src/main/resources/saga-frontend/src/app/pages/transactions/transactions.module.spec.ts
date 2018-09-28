import { TransactionsModule } from './transactions.module';

describe('TransactionsModule', () => {
  let transactionsModule: TransactionsModule;

  beforeEach(() => {
    transactionsModule = new TransactionsModule();
  });

  it('should create an instance', () => {
    expect(transactionsModule).toBeTruthy();
  });
});
