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
