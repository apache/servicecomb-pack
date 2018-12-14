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