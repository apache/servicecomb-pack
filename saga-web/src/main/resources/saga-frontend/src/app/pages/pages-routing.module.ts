import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';

import { PagesComponent } from './pages.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { TransactionsComponent, SuccessTabComponent, FailedTabComponent } from './transactions/transactions.component';
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
    children: [{
      path: '',
      redirectTo: 'transactions',
      pathMatch: 'full',
    }, {
      path: '/successTab',
      component: SuccessTabComponent,
    }, {
      path: '/failedTab',
      component: FailedTabComponent,
    }],
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
