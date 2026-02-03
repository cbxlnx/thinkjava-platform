import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiagnosticPageComponent } from './diagnostic-page.component';

describe('DiagnosticPage', () => {
  let component: DiagnosticPageComponent;
  let fixture: ComponentFixture<DiagnosticPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DiagnosticPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DiagnosticPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
