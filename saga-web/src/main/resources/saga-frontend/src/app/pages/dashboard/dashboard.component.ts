import { Component, OnDestroy, OnInit } from '@angular/core';
import { NbThemeService, NbSpinnerService } from '@nebular/theme';
import { takeWhile } from 'rxjs/operators/takeWhile' ;
import { SagaeventsService } from '../../@core/data/saga-events.service';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';

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
})
export class DashboardComponent implements OnDestroy {

  private alive = true;
  private loading = false;
  succTransactionsArr = [];
  failedTransactionsArr = [];
  successTxSettings: any;
  failedTxSettings: any;
  totalTx: number;
  successTx: number;
  failedTx: number;
  isGlobalTx: boolean = true;

  totalTxCard: CountCardSettings = {
    title: 'Total Transactions',
    footer: 'Last updated few hours ago',
    type: 'info',
    number: 0,
    iconClass: 'fas fa-exchange-alt',
    footerIconClass: 'far fa-clock',
  };

  successTxCard: CountCardSettings = {
    title: 'Successful Transactions',
    footer: 'Last updated few hours ago',
    type: 'success',
    number: 0,
    iconClass: 'far fa-check-circle',
    footerIconClass: 'far fa-clock',
  };

  failedTxCard: CountCardSettings = {
    title: 'Failed Transactions',
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
    this.successTxCard,
    this.failedTxCard,
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
        ...this.successTxCard,
        type: 'success',
      },
      {
        ...this.failedTxCard,
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

    this.getAllEvents();
  }
  refresh(){
    this.getAllEvents();
  }

  getAllEvents() {
    this.loading = true;
    this.events.getAllEvents().subscribe(data => {
      //this.totalTx = data.body.length; // In case if real API response
      this.totalTx = data.length; // In case of static json file
      this.totalTxCard.number = this.totalTx;
      this.succTransactionsArr = _.sortBy(data, function(item){
        return -item['creationTime']; //Sorted by Create time
      });
      this.failedTransactionsArr = _.sortBy(data, function(item){
        return -item['creationTime']; //Sorted by Create time
      }); 
      /* this.succTransactionsArr = _.sortBy(data.body, function(item){
        return -item['creationTime']; //Sorted by Create time
      });
      this.failedTransactionsArr = _.sortBy(data.body, function(item){
        return -item['creationTime']; //Sorted by Create time
      }); */
      this.loading = false;
      this.util.success("Transactions fetched!", "All transactions have been fetched");
    },
    (error) => {
      this.succTransactionsArr = [];
      this.failedTransactionsArr = [];
      this.util.error("Error!", "Something went wrong. The transactions could not be fetched.");
    });
  }

  ngOnDestroy() {
    this.alive = false;
    this.util.clearToasts();
  }
}
