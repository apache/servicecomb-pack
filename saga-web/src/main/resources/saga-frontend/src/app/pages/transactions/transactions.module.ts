import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Ng2SmartTableModule } from 'ng2-smart-table';

import { ThemeModule } from '../../@theme/theme.module';
import { TransactionsComponent, SuccessTabComponent, FailedTabComponent } from './transactions.component';

@NgModule({
  imports: [
    CommonModule,
    ThemeModule,
    NgxEchartsModule,
    NgxChartsModule,
    Ng2SmartTableModule,
  ],
  declarations: [TransactionsComponent , SuccessTabComponent, FailedTabComponent]
})
export class TransactionsModule { }
