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
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';


import { environment } from '../../../environments/environment';

const API_URL = environment.apiUrl;

@Injectable()
export class SagaeventsService {

  private events;

  constructor(private http: HttpClient) {
    
  }

  public getRecentTransactions(status: any, count?: number): Observable<any> {
    let url;
    if(status && status.length){
      if(count){
        url = API_URL + '/saga/recent?status=' + status + '&count=' + count; 
      } else{
        url = API_URL + '/saga/recent?status=' + status + '&count=5';
      }
    }
    return this.http.get(url, {observe: 'response'});
  }

  public findTransaction(gid?: any, name?: string): Observable<any> {
    let url;
      if(gid){
        url = API_URL + '/saga/findTransactions?globalTxID=' + gid ; 
      } else if(name){
        url = API_URL + '/saga/findTransactions?microServiceName=' + name ;
      } else {
        url = API_URL + '/saga/findTransactions';
      }
    return this.http.get(url, {observe: 'response'});
  }

  public getTransactions(status: any): Observable<any> {
    let url;
    if(status && status.length){
      url = API_URL + '/saga/transactions?status=' + status;
    }
    return this.http.get(url, {observe: 'response'});
  }

  public getAllStats(): Observable<any> {
    return this.http.get(API_URL + '/saga/stats', {observe: 'response'});
  }

}
