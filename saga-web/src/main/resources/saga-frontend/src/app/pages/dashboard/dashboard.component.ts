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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NbThemeService, NbSpinnerService } from '@nebular/theme';
import { takeWhile } from 'rxjs/operators/takeWhile' ;
import { SagaeventsService } from '../../@core/data/saga-events.service';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';
import { RecentTableComponent } from './recent-table/recent-table.component';

interface CountCardSettings {
  title: string;
  footer: string;
  number: number;
  iconClass: string;
  footerIconClass: string;
  type: string;
}

@Component({
  selector: 'ngx-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnDestroy {

  private alive = true;
  private loading = false;
  succTransactionsArr = [];
  failedTransactionsArr = [];
  committedTransArr =[];
  pendingTransArr =[];
  compensatingTransArr =[];
  rollbackedTransArr =[];
  successTxSettings: any;
  failedTxSettings: any;
  isGlobalTx: boolean = true;
  committedTransactions: number;
  compensatingTransactions: number;
  failureRate: number;
  pendingTransactions: number;
  rollbackTransactions: number;
  totalTransactions: number;
  updatedAt: any;

  totalTxCard: CountCardSettings = {
    title: 'Total',
    footer: 'Last updated few hours ago',
    type: 'info',
    number: 0,
    iconClass: 'fas fa-exchange-alt',
    footerIconClass: 'far fa-clock',
  };

  committedTxCard: CountCardSettings = {
    title: 'Committed',
    footer: 'Last updated few hours ago',
    type: 'success',
    number: 0,
    iconClass: 'far fa-check-circle',
    footerIconClass: 'far fa-clock',
  };

  pendingTxCard: CountCardSettings = {
    title: 'Pending',
    footer: 'Last updated few hours ago',
    type: 'danger',
    number: 0,
    iconClass: 'fas fa-exclamation-circle',
    footerIconClass: 'far fa-clock',
  };

  compensatingTxCard: CountCardSettings = {
    title: 'Compensating',
    footer: 'Last updated few hours ago',
    type: 'danger',
    number: 0,
    iconClass: 'fas fa-exclamation-circle',
    footerIconClass: 'far fa-clock',
  };

  rollbackTxCard: CountCardSettings = {
    title: 'Rollback',
    footer: 'Last updated few hours ago',
    type: 'danger',
    number: 0,
    iconClass: 'fas fa-exclamation-circle',
    footerIconClass: 'far fa-clock',
  };

  rateTxCard: CountCardSettings = {
    title: 'Failure Rate',
    footer: 'Last updated few hours ago',
    type: 'primary',
    number: 0,
    iconClass: 'fas fa-percent',
    footerIconClass: 'far fa-clock',
  };


  statusCards: string;

  commonStatusCardsSet: CountCardSettings[] = [
    this.totalTxCard,
    this.committedTxCard,
    this.pendingTxCard,
    this.compensatingTxCard,
    this.rollbackTxCard,
    this.rateTxCard,
  ];

  statusCardsByThemes: {
    default: CountCardSettings[];
    cosmic: CountCardSettings[];
    corporate: CountCardSettings[];
  } = {
    default: this.commonStatusCardsSet,
    cosmic: this.commonStatusCardsSet,
    corporate: [
      {
        ...this.totalTxCard,
        type: 'info',
      },
      {
        ...this.committedTxCard,
        type: 'success',
      },
      {
        ...this.pendingTxCard,
        type: 'danger',
      },
      {
        ...this.compensatingTxCard,
        type: 'danger',
      },
      {
        ...this.rollbackTxCard,
        type: 'danger',
      },
      {
        ...this.rateTxCard,
        type: 'primary',
      },
    ],
  };

  constructor(private themeService: NbThemeService, private events: SagaeventsService, private util: UtilService) {
    this.themeService.getJsTheme()
      .pipe(takeWhile(() => this.alive))
      .subscribe(theme => {
        this.statusCards = this.statusCardsByThemes[theme.name];
    });
    this.getAllStats();
  }
  refresh(){
    this.getAllStats();
  }

  //Get all stats
  getAllStats(){
    this.loading = true;
    this.events.getAllStats().subscribe(data =>{
      this.totalTxCard.number = data.body.totalTransactions;
      this.committedTxCard.number = data.body.committedTransactions;
      this.pendingTxCard.number = data.body.pendingTransactions;
      this.compensatingTxCard.number = data.body.compensatingTransactions;
      this.rollbackTxCard.number = data.body.rollbackTransactions;
      this.rateTxCard.number = data.body.failureRate;
      this.updatedAt = new Date(data.body.updatedAt);
      this.loading = false;
    },
    (error) => {
      this.util.error("Error!", "Something went wrong. The stats could not be fetched.");
      this.loading = false;
    });
  }

  ngOnDestroy() {
    this.alive = false;
    this.util.clearToasts();
  }
}
