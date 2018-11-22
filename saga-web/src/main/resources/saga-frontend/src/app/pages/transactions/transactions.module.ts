import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Ng2SmartTableModule } from 'ng2-smart-table';
import { CustomRenderComponent } from './customRender.component';

import { ThemeModule } from '../../@theme/theme.module';
import { TransactionsTableComponent} from './transactionsTable.component';
import { TransactionsComponent} from './transactions.component';
import { FindTransactionComponent } from './findTransaction.component';

@NgModule({
  imports: [
    CommonModule,
    ThemeModule,
    NgxEchartsModule,
    NgxChartsModule,
    Ng2SmartTableModule,
  ],
  entryComponents: [CustomRenderComponent],
  declarations: [TransactionsTableComponent , TransactionsComponent, FindTransactionComponent, CustomRenderComponent]
})
export class TransactionsModule { }
