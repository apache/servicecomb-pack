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
    children: [
      {
        title: 'All Transactions',
        link: '/pages/transactions',
      },
      {
        title: 'Successful Transactions',
        link: '/pages/transactions/successTab',
      },
      {
        title: 'Failed Transactions',
        link: '/pages/transactions/failedTab',
      }
    ],
  },
];
