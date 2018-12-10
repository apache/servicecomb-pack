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
