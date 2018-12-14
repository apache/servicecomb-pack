/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
