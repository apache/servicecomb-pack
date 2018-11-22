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
