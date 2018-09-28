import { Component } from '@angular/core';

@Component({
  selector: 'ngx-footer',
  styleUrls: ['./footer.component.scss'],
  template: `
    <span class="created-by">&copy; 2018 <b><a href="http://servicecomb.apache.org/" target="_blank">ServiceComb</a></b></span>
    <div class="socials">
      <a title="GitHub" href="https://github.com/apache?q=ServiceComb" target="_blank" class="ion ion-social-github"></a>
      <a title="Twitter" href="https://twitter.com/servicecomb" target="_blank" class="ion ion-social-twitter"></a>
      <a title="Gitter" href="https://gitter.im/ServiceCombUsers/Saga" target="_blank" class="fab fa-gitter"></a>
    </div>
  `,
})
export class FooterComponent {
}
