
import { Component, Input, OnInit } from '@angular/core';
import { NavigationEnd, ActivatedRoute, Router } from '@angular/router';
import { ViewCell } from 'ng2-smart-table';

@Component({
  template: `
    <a href="javascript:void(0)" title="View Transactions" class="nav-link" (click)="showDetails(value)">{{value}}</a>
  `,
})
export class CustomRenderComponent implements ViewCell, OnInit {

  renderValue: string;

  @Input() value: string | number;
  @Input() rowData: any;

  constructor(private router: Router){}

  ngOnInit() {
  }
  showDetails(gid: any){
    if(this.value=== this.rowData.globalTxId){
      this.router.navigate(['/pages/find'], {queryParams: {gid: this.value}});
    } 
    if(this.value === this.rowData.serviceName){
      this.router.navigate(['/pages/find'], {queryParams: {serviceName: this.rowData.serviceName}});
    }
    
  }

}