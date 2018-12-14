/**
 * @license
 * Copyright Akveo. All Rights Reserved.
 * Licensed under the MIT License. See licenses/LICENSE-ngxadmin for license information.
 */
import { NgModule } from '@angular/core';
import { ThemeModule } from '../../@theme/theme.module';
import { MiscellaneousRoutingModule, routedComponents } from './miscellaneous-routing.module';

@NgModule({
  imports: [
    ThemeModule,
    MiscellaneousRoutingModule,
  ],
  declarations: [
    ...routedComponents,
  ],
})
export class MiscellaneousModule { }
