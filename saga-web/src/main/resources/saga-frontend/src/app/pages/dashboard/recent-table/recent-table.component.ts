import { Component, OnInit, Input } from '@angular/core';
import { SagaeventsService } from '../../../@core/data/saga-events.service';
import { UtilService } from '../../../@core/utils/util.service';
import { NavigationEnd, ActivatedRoute, Router } from '@angular/router';
import * as _ from 'underscore';

@Component({
  selector: 'ngx-recent-table',
  templateUrl: './recent-table.component.html',
  styleUrls: ['./recent-table.component.scss']
})
export class RecentTableComponent implements OnInit {
  @Input() transactionStatus: string;
  @Input() cardType: string;
  @Input() showTitle: boolean;
  @Input() showActions: boolean;
  private loading = false;
  recTransactionsArr: any;
  txCount: number;


  constructor(private events: SagaeventsService, private util: UtilService, private router: Router) {
   }

  ngOnInit() {
    this.getRecentTransactions(this.transactionStatus);
  }

  refreshTable(){
    this.getRecentTransactions(this.transactionStatus);
  }

  showAll(status: any){
    let url = '/pages/transactions/' + status.toLowerCase();
    this.router.navigate([url], {queryParams: {type: status}});
  }

  getRecentTransactions(status: any){
    this.loading = true;
    this.events.getRecentTransactions(status).subscribe(data => {
      this.recTransactionsArr = _.sortBy(data.body, function(item){
        return -item['creationTime']; //Sorted by Create time
      });
      this.txCount = data.body.length;
      this.loading = false;
      let message = 'All recent ' + status + ' transactions have been fetched.';
      this.util.success("", message);
    },
    (error) => {
      this.recTransactionsArr = [];
      let message = 'Something went wrong. The ' + status + ' transactions could not be fetched.';
      this.util.error("Error!", message);
    });
  }

}
