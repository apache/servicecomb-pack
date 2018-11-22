import {Router, ActivatedRoute, Params} from '@angular/router';
import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { SagaeventsService } from '../../@core/data/saga-events.service';
import {TransactionsTableComponent} from './transactionsTable.component';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';

@Component({
    selector: 'ngx-find-transactions',
    templateUrl: './findTransaction.component.html',
  })
  export class FindTransactionComponent implements OnInit { 
    transactionsArr = [];
    type: any;
    globalID: any;
    serviceName: any;
    hideGID: boolean;
    hideName: boolean;
    loading: boolean;

    constructor(private events: SagaeventsService, private util: UtilService, private activatedRoute: ActivatedRoute){
        this.activatedRoute.queryParams.subscribe(params => {
          this.globalID = params['gid'];
        });
      }

      findTransaction(gid?:any, name?: any) {
        this.loading = true;
        this.util.clearToasts();
        this.events.findTransaction(gid, name).subscribe(data => {
          this.transactionsArr = data.body; 
          if(this.transactionsArr.length){
            this.util.success("", "All transactions have been fetched");
          }
          this.loading = false;
        },
        (error) => {
          this.util.error("Error!", "Something went wrong. The transactions could not be fetched.");
          this.loading = false;
        });
      }
      resetForm(){
          this.transactionsArr = [];
          this.globalID = "";
          this.serviceName = "";
          this.hideGID = false;
          this.hideName = false;
      }

      ngOnInit() {
        this.activatedRoute.queryParams.subscribe(params => {
            this.globalID = params['gid'];
            this.serviceName = params['serviceName'];
          if(params['gid']){
             this.findTransaction(params['gid']);
          }
          if(params['serviceName']){
            this.findTransaction(undefined, params['serviceName']);
          }
        });
      }
  }