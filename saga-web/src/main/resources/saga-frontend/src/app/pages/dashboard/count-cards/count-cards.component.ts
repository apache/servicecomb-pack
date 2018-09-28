import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'ngx-count-card',
  templateUrl: './count-cards.component.html',
  styleUrls: ['./count-cards.component.scss']
})
export class CountCardsComponent implements OnInit {

  @Input() title: string;
  @Input() footer: string;
  @Input() number: number;
  @Input() iconClass: string;
  @Input() footerIconClass: string;
  @Input() type: string;

  constructor() { }

  ngOnInit() {
  }

}
