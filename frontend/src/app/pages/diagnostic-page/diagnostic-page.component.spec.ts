import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { DiagnosticPageComponent } from './diagnostic-page.component';
import { DiagnosticService } from '../../core/diagnostic/diagnostic.service';
import { Question } from './questions.mock';

describe('DiagnosticPageComponent', () => {
  // the component under test
  let component: DiagnosticPageComponent;

  // gives access to the angular test wrapper around the component
  let fixture: ComponentFixture<DiagnosticPageComponent>;

  // mocks the backend diagnostic api calls
  let diagnosticSpy: jasmine.SpyObj<DiagnosticService>;

  // mocks navigation after diagnostic actions
  let routerSpy: jasmine.SpyObj<Router>;

  // a small deterministic question set used by the scoring and submission tests
  const testQuestions: Question[] = [
    {
      id: 1,
      checkpoint: 'fundamentals',
      difficulty: 1,
      prompt: 'Fundamentals question',
      optionA: 'A1',
      optionB: 'B1',
      optionC: 'C1',
      optionD: 'D1',
      correctOption: 'A',
    },
    {
      id: 2,
      checkpoint: 'loops',
      difficulty: 1,
      prompt: 'Loops question',
      optionA: 'A2',
      optionB: 'B2',
      optionC: 'C2',
      optionD: 'D2',
      correctOption: 'B',
    },
    {
      id: 3,
      checkpoint: 'arrays',
      difficulty: 2,
      prompt: 'Arrays question',
      optionA: 'A3',
      optionB: 'B3',
      optionC: 'C3',
      optionD: 'D3',
      correctOption: 'C',
    },
  ];

  beforeEach(async () => {
    // creates spies for the component dependencies
    diagnosticSpy = jasmine.createSpyObj<DiagnosticService>('DiagnosticService', ['complete']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    // sets up the standalone component with mocked providers
    await TestBed.configureTestingModule({
      imports: [DiagnosticPageComponent],
      providers: [
        { provide: DiagnosticService, useValue: diagnosticSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    // creates the component instance and runs initial change detection
    fixture = TestBed.createComponent(DiagnosticPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    // verifies the component initializes successfully
    expect(component).toBeTruthy();
  });

  it('starts the quiz and loads the first question', () => {
    // starts a fresh diagnostic attempt
    component.start();

    // verifies quiz mode begins and a question is ready
    expect(component.step).toBe('QUIZ');
    expect(component.currentQuestion).toBeTruthy();
    expect(component.totalQuestionsPlanned).toBeGreaterThan(0);
  });

  it('calculates score percent from attempted answers only', () => {
    // injects a known question set and answer pattern
    component.selectedPool = testQuestions;
    component.answers = {
      1: 'A',
      2: 'SKIP',
      3: 'C',
    };

    // verifies skipped questions still count as attempted but not correct
    expect(component.scorePercent).toBe(67);
  });

  it('submits diagnostic results and switches to the result view', () => {
    // mocks a successful backend completion response
    diagnosticSpy.complete.and.returnValue(
      of({
        fundamentals: 'Strong',
        loops: 'Weak',
        arrays: 'Strong',
        methods: 'Unknown',
        oop: 'Unknown',
        startModule: 'loops',
        diagnosticPercent: 67,
      })
    );

    // sets up one incorrect answer so review items can be checked
    component.selectedPool = testQuestions;
    component.answers = {
      1: 'A',
      2: 'A',
      3: 'C',
    };

    // submits the computed diagnostic result
    component.submit();

    // verifies the backend payload matches the calculated topic levels
    expect(diagnosticSpy.complete).toHaveBeenCalledWith({
      fundamentals: 'Strong',
      loops: 'Weak',
      arrays: 'Strong',
      methods: 'Unknown',
      oop: 'Unknown',
      startModule: 'loops',
      diagnosticPercent: 67,
    });

    // verifies the component moves into result mode and builds review data
    expect(component.step).toBe('RESULT');
    expect(component.resultJson.fundamentals).toBe('Strong');
    expect(component.reviewItems.length).toBe(1);
    expect(component.reviewItems[0].selectedText).toBe('A: A2');
    expect(component.reviewItems[0].correctText).toBe('B: B2');
  });

  it('retake resets result state back to the intro step', () => {
    // seeds the component with result state from a previous run
    component.step = 'RESULT';
    component.currentQuestion = testQuestions[0];
    component.selectedPool = testQuestions;
    component.answers = { 1: 'A' };
    component.resultJson = { fundamentals: 'Strong' };
    component.reviewItems = [
      {
        id: 1,
        prompt: 'Question',
        checkpoint: 'fundamentals',
        difficulty: 1,
        selectedAnswer: 'B',
        correctAnswer: 'A',
        selectedText: 'B',
        correctText: 'A',
        isSkipped: false,
      },
    ];

    // restarts the diagnostic flow
    component.retake();

    // verifies the component returns to a clean intro state
    expect(component.step).toBe('INTRO');
    expect(component.currentQuestion).toBeNull();
    expect(component.selectedPool).toEqual([]);
    expect(component.answers).toEqual({});
    expect(component.resultJson).toBeNull();
    expect(component.reviewItems).toEqual([]);
  });

  it('navigates to the dashboard when viewing the path', () => {
    // triggers the post-result navigation action
    component.viewPath();

    // verifies the component routes the user to the dashboard
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
