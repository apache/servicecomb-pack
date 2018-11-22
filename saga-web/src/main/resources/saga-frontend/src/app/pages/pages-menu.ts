import { NbMenuItem } from '@nebular/theme';

export const MENU_ITEMS: NbMenuItem[] = [
  {
    title: 'Dashboard',
    icon: 'icon ion-speedometer',
    link: '/pages/dashboard',
    home: true,
  },
  {
    title: 'Saga Transactions',
    icon: 'fas fa-exchange-alt',
    link: '/pages/transactions',
    expanded: false,
    children: [
      {
        title: 'All Transactions',
        link: '/pages/transactions',
        queryParams: {type: "ALL"},
      },
      {
        title: 'Committed Transactions',
        link: '/pages/transactions/committed',
        queryParams: {type: "COMMITTED"},
      },
      {
        title: 'Pending Transactions',
        link: '/pages/transactions/pending',
        queryParams: {type: "PENDING"},
      },
      {
        title: 'Compensating Transactions',
        link: '/pages/transactions/compensating',
        queryParams: {type: "COMPENSATING"},
      },
      {
        title: 'Rollback Transactions',
        link: '/pages/transactions/rollbacked',
        queryParams: {type: "ROLLBACKED"},
      }
    ],
  },{
    title: 'Find Transactions',
    icon: 'icon ion-search',
    link: '/pages/find',
  },
];
