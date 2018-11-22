import {Router, ActivatedRoute, Params} from '@angular/router';
import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { SagaeventsService } from '../../@core/data/saga-events.service';
import {TransactionsTableComponent} from './transactionsTable.component';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';

@Component({
    selector: 'ngx-all-transactions',
    templateUrl: './transactions.component.html',
  })
  export class TransactionsComponent implements OnInit { 
    transactionsArr = [];
    type: any;
      constructor(private events: SagaeventsService, private util: UtilService, private activatedRoute: ActivatedRoute){
        this.activatedRoute.queryParams.subscribe(params => {
          this.type = params['type'];
        });
        this.getAllTransactions(this.type);
      }

      getAllTransactions(status: any) {
        this.events.getTransactions(status).subscribe(data => {
          this.transactionsArr = data.body; 
          this.util.success("Transactions fetched!", "All transactions have been fetched");
        },
        (error) => {
          this.util.error("Error!", "Something went wrong. The transactions could not be fetched.");
        });
      }

      ngOnInit() {
        this.activatedRoute.queryParams.subscribe(params => {
          this.type = params['type'];
        });
      }
  }
