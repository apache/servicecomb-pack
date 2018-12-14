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
