import { Component } from '@angular/core';

import { MENU_ITEMS } from './pages-menu';

@Component({
  selector: 'ngx-pages',
  template: `
  <ngx-default-layout>
  <nb-menu [items]="menu"></nb-menu>
  
  <router-outlet></router-outlet>
  </ngx-default-layout>
  `,
})
export class PagesComponent {

  menu = MENU_ITEMS;
}
