/**
 * @license
 * Copyright Akveo. All Rights Reserved.
 * Licensed under the MIT License. See licenses/LICENSE-ngxadmin for license information.
 */
import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SagaeventsService } from './saga-events.service';
import { StateService } from './state.service';

const SERVICES = [
  SagaeventsService,
  StateService,
];

@NgModule({
  imports: [
    CommonModule,
  ],
  providers: [
    ...SERVICES,
  ],
})
export class DataModule {
  static forRoot(): ModuleWithProviders {
    return <ModuleWithProviders>{
      ngModule: DataModule,
      providers: [
        ...SERVICES,
      ],
    };
  }
}
