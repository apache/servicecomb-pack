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
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';

import { PagesComponent } from './pages.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import {TransactionsComponent} from './transactions/transactions.component';
import {FindTransactionComponent} from './transactions/findTransaction.component'
import { NotFoundComponent } from './miscellaneous/not-found/not-found.component';

const routes: Routes = [{
  path: '',
  component: PagesComponent,
  children: [{
    path: 'dashboard',
    component: DashboardComponent,
  },  {
    path: 'transactions',
    component: TransactionsComponent,
  },{
    path: 'transactions/committed',
    component: TransactionsComponent,
  },
  {
    path: 'transactions/pending',
    component: TransactionsComponent,
  },
  {
    path: 'transactions/compensating',
    component: TransactionsComponent,
  },
  {
    path: 'transactions/rollbacked',
    component: TransactionsComponent,
  },
  {
    path: 'find',
    component: FindTransactionComponent,
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  }, {
    path: '**',
    component: NotFoundComponent,
  }],
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PagesRoutingModule {
}
