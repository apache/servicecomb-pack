import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { NbThemeService } from '@nebular/theme';
import { takeWhile } from 'rxjs/operators/takeWhile' ;
import { SagaeventsService } from '../../@core/data/saga-events.service';
import { UtilService } from '../../@core/utils/util.service';
import * as _ from 'underscore';

@Component({
  selector: 'ngx-success-tab',
  templateUrl: './successTab.component.html',
})
export class SuccessTabComponent { 
    constructor(){}
}

@Component({
  selector: 'ngx-failed-tab',
  templateUrl: './failedTab.component.html',
})
export class FailedTabComponent {
  constructor(){}
 }

@Component({
  selector: 'ngx-transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.scss']
})
export class TransactionsComponent implements OnInit, OnDestroy {

  private alive = true;
  transactionsArr = [];
  settings: any;

  constructor(private events: SagaeventsService, private util: UtilService) { }

  ngOnInit() {
    this.getAllEvents();
    this.setTransactionsTable();
  }

  getAllEvents() {
    this.events.getAllEvents().subscribe(data => {
      console.log('fetched all transactions',data.body);
    //this.transactionsArr = data.body; //In case of real API response
      this.transactionsArr = data;
      this.util.success("Transactions fetched!", "All transactions have been fetched");
    },
    (error) => {
      this.util.error("Error!", "Something went wrong. The transactions could not be fetched.");
    });
  }
  refresh(){
    this.getAllEvents();
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
          width: '10%',
        },
        instanceId: {
          title: 'Instance ID',
          editable: false,
          width: '10%',
        },
        globalTxId: {
          title: 'Global ID',
          filter: true,
          sort: true,
          
          editable: false
        },
        localTxId: {
          title: 'Local ID',
          filter: true,
          sort: true,
          
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
