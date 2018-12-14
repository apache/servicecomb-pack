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
import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl, SafeUrl} from '@angular/platform-browser';
import { RouterModule, Routes, Router } from '@angular/router';
import { NbThemeService } from '@nebular/theme';
import { takeWhile } from 'rxjs/operators/takeWhile' ;
import { SagaeventsService } from '../../@core/data/saga-events.service';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';

import { CustomRenderComponent } from './customRender.component';



@Component({
  selector: 'ngx-transactions-table',
  templateUrl: './transactionsTable.component.html',
  styleUrls: ['./transactionsTable.component.scss']
})
export class TransactionsTableComponent implements OnInit, OnDestroy {
  @Input() tableData: any;
  @Input() hideRefresh: boolean;
  @Input() showTitle: boolean;
  @Input() title: string;
  @Input() showCount: boolean;

  private alive = true;
  transactions: any;
  settings: any;

  constructor(private events: SagaeventsService, private util: UtilService, private router: Router, private sanitizer: DomSanitizer) { }

  ngOnInit() {
    this.transactions = this.tableData;
    this.setTransactionsTable();
  }

  refresh(){
    this. setTransactionsTable();
  }

  setTransactionsTable() {
    this.settings = {
      pager: {
        perPage: 10,
      },
      add: {
        addButtonContent: '<i class="nb-plus"></i>',
        createButtonContent: '<i class="nb-checkmark"></i>',
        cancelButtonContent: '<i class="nb-close"></i>',
      },
      edit: {
        editButtonContent: '<i class="nb-edit"></i>',
        saveButtonContent: '<i class="nb-checkmark"></i>',
        cancelButtonContent: '<i class="nb-close"></i>',
      },
      delete: {
        deleteButtonContent: '<i class="nb-trash"></i>',
        confirmDelete: true,
      },
      actions: {
        add: false,
        edit: false,
        delete: false,
        position: 'right',
      },
      columns: {
        surrogateId: {
          title: 'ID',
          filter: true,
          sort: true,
          editable: false,
          width: '5%',
        },
        serviceName: {
          title: 'Service Name',
          filter: true,
          sort: true,
          editable: false,
          width: '9%',
          type:'custom',
          renderComponent: CustomRenderComponent,
        },
        instanceId: {
          title: 'Instance ID',
          editable: false,
          width: '11%',
        },
        globalTxId: {
          title: 'Global ID',
          filter: true,
          sort: true,
          width: '16%',
          editable: false,
          type:'custom',
          renderComponent: CustomRenderComponent,
        },
        localTxId: {
          title: 'Local ID',
          filter: true,
          sort: true,
          width: '15%',
          editable: false
        },
        compensationMethod: {
          title: 'Compensation Method',
          editable: false,
          width: '20%',
        },
      },

    };
  }

  ngOnDestroy() {
    this.alive = false;
    this.util.clearToasts();
  }

}
