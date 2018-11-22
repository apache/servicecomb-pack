import { NgModule } from '@angular/core';
import { NgxEchartsModule } from 'ngx-echarts';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Ng2SmartTableModule } from 'ng2-smart-table';

import { ThemeModule } from '../../@theme/theme.module';
import { DashboardComponent } from './dashboard.component';

import { ChartModule } from 'angular2-chartjs';
import { CountCardsComponent } from './count-cards/count-cards.component';
import { RecentTableComponent } from './recent-table/recent-table.component';

@NgModule({
  imports: [
    ThemeModule,
    ChartModule,
    NgxEchartsModule,
    NgxChartsModule,
    Ng2SmartTableModule,
  ],
  declarations: [
    DashboardComponent,
    CountCardsComponent,
    RecentTableComponent,
  ],
  providers: [
  ],
})
export class DashboardModule { }
