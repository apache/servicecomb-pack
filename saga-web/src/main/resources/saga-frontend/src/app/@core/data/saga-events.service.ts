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

  public getAllEvents(): Observable<any> {
    return this.http.get('../../../assets/data/events.json');
/*     return this.http.get(API_URL + '/events', {observe: 'response'}); */
  }

  public getSagaEvents(type: string): Observable<any> {
    let url = "";
    if(type=="success"){
      url = API_URL + '/events?type=success';
    } else if(type=="failed"){
      url = API_URL + '/events?type=failed';
    } else {
      url = API_URL + '/events';
    }
    return this.http.get(url, {observe: 'response'});
  }
}
