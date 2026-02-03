export const MAX_QUESTIONS = 10;

export const CHECKPOINTS = ['fundamentals', 'loops', 'arrays', 'methods', 'oop'] as const;
export type Checkpoint = typeof CHECKPOINTS[number];

export type Difficulty = 1 | 2 | 3;
export type AnswerOption = 'A' | 'B' | 'C' | 'D';

export type Question = {
  id: number;
  checkpoint: Checkpoint;
  difficulty: Difficulty;
  isGate?: boolean;
  subskill?: string;
  explanation?: string;

  prompt: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctOption: AnswerOption;
};

// Proportions: 2 questions per checkpoint => 10 total.
export const QUIZ_QUOTA: Record<Checkpoint, number> = {
  fundamentals: 2,
  loops: 2,
  arrays: 2,
  methods: 2,
  oop: 2,
};

export const DIAGNOSTIC_QUESTIONS: Question[] = [
  // ========================================================================
  // Fundamentals (syntax, types, operators, conditionals)
  // ========================================================================

  {
    id: 1,
    checkpoint: 'fundamentals',
    difficulty: 1,
    isGate: true,
    subskill: 'numeric-promotion',
    prompt: `int x = 7;
double y = 2;
System.out.println(x / y);`,
    optionA: '3',
    optionB: '3.5',
    optionC: '3.0',
    optionD: 'Compilation error',
    correctOption: 'B',
    explanation: 'Because y is double, x is promoted to double and the result is 3.5.',
  },
  {
    id: 2,
    checkpoint: 'fundamentals',
    difficulty: 1,
    isGate: true,
    subskill: 'if-else-chain',
    prompt: `int a = 5;
int b = 8;

if (a > b) System.out.print("A");
else if (a == b) System.out.print("E");
else System.out.print("B");`,
    optionA: 'A',
    optionB: 'E',
    optionC: 'B',
    optionD: 'Nothing prints',
    correctOption: 'C',
    explanation: 'a is less than b, so the else branch executes and prints B.',
  },
  {
    id: 11,
    checkpoint: 'fundamentals',
    difficulty: 1,
    subskill: 'variable-declaration',
    prompt: 'Which line correctly declares an integer variable in Java?',
    optionA: 'var x = 10;',
    optionB: 'int x = 10;',
    optionC: 'integer x := 10;',
    optionD: 'declare x = 10;',
    correctOption: 'B',
    explanation: '`int x = 10;` is the correct Java syntax.',
  },
  {
    id: 12,
    checkpoint: 'fundamentals',
    difficulty: 2,
    subskill: 'string-equality',
    prompt: `What is the best way to compare two Strings for content equality in Java?`,
    optionA: 'a == b',
    optionB: 'a.equals(b)',
    optionC: 'a = b',
    optionD: 'a.compareTo(b) == 1',
    correctOption: 'B',
    explanation: '`equals()` checks content; `==` checks reference identity.',
  },
  {
    id: 13,
    checkpoint: 'fundamentals',
    difficulty: 2,
    subskill: 'logical-operators',
    prompt: `boolean a = true;
boolean b = false;
System.out.println(a && !b);`,
    optionA: 'true',
    optionB: 'false',
    optionC: 'Compilation error',
    optionD: 'Runtime error',
    correctOption: 'A',
    explanation: '`!b` is true, so `true && true` is true.',
  },
  {
    id: 14,
    checkpoint: 'fundamentals',
    difficulty: 3,
    subskill: 'casting-truncation',
    prompt: `double d = 9.9;
int x = (int) d;
System.out.println(x);`,
    optionA: '9.9',
    optionB: '10',
    optionC: '9',
    optionD: 'Compilation error',
    correctOption: 'C',
    explanation: 'Casting double to int truncates (drops) the decimal part.',
  },
  {
    id: 15,
    checkpoint: 'fundamentals',
    difficulty: 3,
    subskill: 'switch-rules',
    prompt: 'Which type is NOT allowed as a switch expression in modern Java?',
    optionA: 'int',
    optionB: 'String',
    optionC: 'boolean',
    optionD: 'enum',
    correctOption: 'C',
    explanation: 'Switch supports int/char/String/enum (and some wrappers), but not boolean.',
  },

  // ========================================================================
  // Loops (for/while, boundaries, break/continue, counting)
  // ========================================================================

  {
    id: 3,
    checkpoint: 'loops',
    difficulty: 1,
    isGate: true,
    subskill: 'for-loop-accumulator',
    prompt: `int sum = 0;
for (int i = 1; i <= 3; i++) {
  sum += i;
}
System.out.println(sum);`,
    optionA: '3',
    optionB: '6',
    optionC: '7',
    optionD: '0',
    correctOption: 'B',
    explanation: 'The loop adds 1 + 2 + 3, resulting in 6.',
  },
  {
    id: 4,
    checkpoint: 'loops',
    difficulty: 2,
    subskill: 'while-loop-boundary',
    prompt: `int i = 0;
while (i < 3) {
  System.out.print(i);
  i++;
}`,
    optionA: '012',
    optionB: '123',
    optionC: '0123',
    optionD: 'Infinite loop',
    correctOption: 'A',
    explanation: 'i starts at 0 and increments until it reaches 3 (exclusive).',
  },
  {
    id: 16,
    checkpoint: 'loops',
    difficulty: 1,
    subskill: 'loop-purpose',
    prompt: 'Which loop is typically best when you know exactly how many times you want to iterate?',
    optionA: 'for loop',
    optionB: 'while loop',
    optionC: 'do-while loop',
    optionD: 'recursive loop',
    correctOption: 'A',
    explanation: 'A for-loop is commonly used when iteration count is known.',
  },
  {
    id: 17,
    checkpoint: 'loops',
    difficulty: 2,
    subskill: 'break-continue',
    prompt: `for (int i = 1; i <= 5; i++) {
  if (i == 3) continue;
  System.out.print(i);
}`,
    optionA: '12345',
    optionB: '1245',
    optionC: '12',
    optionD: 'Infinite loop',
    correctOption: 'B',
    explanation: '`continue` skips printing when i == 3.',
  },
  {
    id: 18,
    checkpoint: 'loops',
    difficulty: 2,
    subskill: 'off-by-one',
    prompt: `int count = 0;
for (int i = 0; i <= 4; i++) {
  count++;
}
System.out.println(count);`,
    optionA: '4',
    optionB: '5',
    optionC: '6',
    optionD: 'Compilation error',
    correctOption: 'B',
    explanation: 'i takes values 0..4 inclusive => 5 iterations.',
  },
  {
    id: 19,
    checkpoint: 'loops',
    difficulty: 3,
    subskill: 'nested-loops',
    prompt: `int x = 0;
for (int i = 1; i <= 2; i++) {
  for (int j = 1; j <= 3; j++) {
    x++;
  }
}
System.out.println(x);`,
    optionA: '2',
    optionB: '3',
    optionC: '5',
    optionD: '6',
    correctOption: 'D',
    explanation: '2 * 3 iterations => 6 increments.',
  },
  {
    id: 20,
    checkpoint: 'loops',
    difficulty: 3,
    subskill: 'do-while',
    prompt: `int i = 10;
do {
  System.out.print(i);
  i++;
} while (i < 10);`,
    optionA: 'Prints nothing',
    optionB: '10',
    optionC: '1011',
    optionD: 'Infinite loop',
    correctOption: 'B',
    explanation: 'do-while runs at least once before checking the condition.',
  },

  // ========================================================================
  // Arrays (indexing, length, loops, common mistakes)
  // ========================================================================

  {
    id: 5,
    checkpoint: 'arrays',
    difficulty: 2,
    subskill: 'indexing-length',
    prompt: `int[] arr = {10, 20, 30, 40};
System.out.println(arr[1] + arr[arr.length - 1]);`,
    optionA: '50',
    optionB: '60',
    optionC: '30',
    optionD: 'Compilation error',
    correctOption: 'B',
    explanation: 'arr[1] is 20 and arr[length - 1] is 40.',
  },
  {
    id: 6,
    checkpoint: 'arrays',
    difficulty: 2,
    subskill: 'traversal-sum',
    prompt: `int[] a = {1, 2, 3};
int s = 0;

for (int i = 0; i < a.length; i++) {
  s += a[i];
}
System.out.println(s);`,
    optionA: '3',
    optionB: '6',
    optionC: '7',
    optionD: 'Index out of bounds',
    correctOption: 'B',
    explanation: 'The loop sums all array elements: 1 + 2 + 3.',
  },
  {
    id: 21,
    checkpoint: 'arrays',
    difficulty: 1,
    subskill: 'array-length',
    prompt: `int[] nums = new int[5];
System.out.println(nums.length);`,
    optionA: '0',
    optionB: '4',
    optionC: '5',
    optionD: 'Compilation error',
    correctOption: 'C',
    explanation: 'Array length is fixed at creation time.',
  },
  {
    id: 22,
    checkpoint: 'arrays',
    difficulty: 1,
    subskill: 'default-values',
    prompt: 'What is the default value of elements in a newly created int[] array?',
    optionA: 'null',
    optionB: '0',
    optionC: '1',
    optionD: 'undefined',
    correctOption: 'B',
    explanation: 'Primitive int elements default to 0.',
  },
  {
    id: 23,
    checkpoint: 'arrays',
    difficulty: 2,
    subskill: 'enhanced-for',
    prompt: `int[] a = {2, 4, 6};
int sum = 0;
for (int x : a) {
  sum += x;
}
System.out.println(sum);`,
    optionA: '6',
    optionB: '10',
    optionC: '12',
    optionD: 'Compilation error',
    correctOption: 'C',
    explanation: '2 + 4 + 6 = 12.',
  },
  {
    id: 24,
    checkpoint: 'arrays',
    difficulty: 2,
    subskill: 'common-error',
    prompt: 'Which statement causes ArrayIndexOutOfBoundsException for int[] a = {1,2,3};?',
    optionA: 'a[0] = 9;',
    optionB: 'a[a.length - 1] = 9;',
    optionC: 'a[3] = 9;',
    optionD: 'a[2] = 9;',
    correctOption: 'C',
    explanation: 'Valid indices are 0..2; index 3 is out of bounds.',
  },
  {
    id: 25,
    checkpoint: 'arrays',
    difficulty: 3,
    subskill: 'two-dimensional',
    prompt: `int[][] m = new int[2][3];
System.out.println(m[0].length);`,
    optionA: '2',
    optionB: '3',
    optionC: '5',
    optionD: 'Compilation error',
    correctOption: 'B',
    explanation: 'm[0] is a row with 3 columns.',
  },

  // ========================================================================
  // Methods (params, return, overloading, static, scope)
  // ========================================================================

  {
    id: 7,
    checkpoint: 'methods',
    difficulty: 2,
    subskill: 'return-composition',
    prompt: `static int f(int x) {
  return x * 2;
}

public static void main(String[] args) {
  int v = f(3) + f(1);
  System.out.println(v);
}`,
    optionA: '6',
    optionB: '8',
    optionC: '10',
    optionD: 'Compilation error',
    correctOption: 'B',
    explanation: 'f(3) returns 6 and f(1) returns 2.',
  },
  {
    id: 8,
    checkpoint: 'methods',
    difficulty: 3,
    subskill: 'parameter-scope',
    prompt: `static int x = 5;

static void change(int x) {
  x = x + 1;
}

public static void main(String[] args) {
  change(x);
  System.out.println(x);
}`,
    optionA: '5',
    optionB: '6',
    optionC: '0',
    optionD: 'Compilation error',
    correctOption: 'A',
    explanation: 'The parameter x shadows the static field; the field is unchanged.',
  },
  {
    id: 26,
    checkpoint: 'methods',
    difficulty: 1,
    subskill: 'return-vs-void',
    prompt: 'What does a method declared with `void` return?',
    optionA: '0',
    optionB: 'null',
    optionC: 'It returns nothing',
    optionD: 'It returns an empty String',
    correctOption: 'C',
    explanation: 'void means no return value.',
  },
  {
    id: 27,
    checkpoint: 'methods',
    difficulty: 2,
    subskill: 'overloading',
    prompt: 'Which best describes method overloading in Java?',
    optionA: 'Same name, different parameter list',
    optionB: 'Same name, different return type only',
    optionC: 'Different name, same parameters',
    optionD: 'Same name, same parameters, different body',
    correctOption: 'A',
    explanation: 'Overloading requires different parameter lists (types/order/count).',
  },
  {
    id: 28,
    checkpoint: 'methods',
    difficulty: 2,
    subskill: 'pass-by-value',
    prompt: `static void inc(int x) { x++; }

public static void main(String[] args) {
  int a = 1;
  inc(a);
  System.out.println(a);
}`,
    optionA: '1',
    optionB: '2',
    optionC: '0',
    optionD: 'Compilation error',
    correctOption: 'A',
    explanation: 'Java is pass-by-value; the original `a` is unchanged.',
  },
  {
    id: 29,
    checkpoint: 'methods',
    difficulty: 3,
    subskill: 'static-context',
    prompt: 'Why can’t a static method directly access instance fields?',
    optionA: 'Because static methods run faster',
    optionB: 'Because there may be no instance (no `this`)',
    optionC: 'Because instance fields are always private',
    optionD: 'Because Java forbids classes with instance fields',
    correctOption: 'B',
    explanation: 'Static methods belong to the class, not a specific object instance.',
  },

  // ========================================================================
  // OOP (classes, constructors, inheritance, polymorphism, encapsulation)
  // ========================================================================

  {
    id: 9,
    checkpoint: 'oop',
    difficulty: 2,
    subskill: 'object-state',
    prompt: `class Counter {
  int value = 0;
  void inc() { value++; }
}

public class Main {
  public static void main(String[] args) {
    Counter c = new Counter();
    c.inc();
    c.inc();
    System.out.println(c.value);
  }
}`,
    optionA: '0',
    optionB: '1',
    optionC: '2',
    optionD: 'Compilation error',
    correctOption: 'C',
    explanation: 'The same object is modified twice, so value becomes 2.',
  },
  {
    id: 10,
    checkpoint: 'oop',
    difficulty: 3,
    subskill: 'dynamic-dispatch',
    prompt: `class A {
  String speak() { return "A"; }
}

class B extends A {
  String speak() { return "B"; }
}

public class Main {
  public static void main(String[] args) {
    A obj = new B();
    System.out.println(obj.speak());
  }
}`,
    optionA: 'A',
    optionB: 'B',
    optionC: 'Compilation error',
    optionD: 'Runtime error',
    correctOption: 'B',
    explanation: 'Method calls are resolved at runtime based on the object type.',
  },
  {
    id: 30,
    checkpoint: 'oop',
    difficulty: 1,
    subskill: 'class-vs-object',
    prompt: 'Which statement is correct?',
    optionA: 'A class is an instance of an object',
    optionB: 'An object is a blueprint for a class',
    optionC: 'A class is a blueprint; an object is an instance',
    optionD: 'Classes and objects are the same thing',
    correctOption: 'C',
    explanation: 'Class = blueprint, object = instance created from that blueprint.',
  },
  {
    id: 31,
    checkpoint: 'oop',
    difficulty: 2,
    subskill: 'constructor-basics',
    prompt: `class Person {
  String name;
  Person(String n) { name = n; }
}

public class Main {
  public static void main(String[] args) {
    Person p = new Person("Ana");
    System.out.println(p.name);
  }
}`,
    optionA: 'null',
    optionB: 'Ana',
    optionC: 'Compilation error',
    optionD: 'Runtime error',
    correctOption: 'B',
    explanation: 'Constructor assigns the field `name` to "Ana".',
  },
  {
    id: 32,
    checkpoint: 'oop',
    difficulty: 2,
    subskill: 'encapsulation',
    prompt: 'What is encapsulation in OOP?',
    optionA: 'Using multiple classes in one file',
    optionB: 'Hiding data by restricting direct access and exposing methods',
    optionC: 'Calling methods from parent classes',
    optionD: 'Overriding a method',
    correctOption: 'B',
    explanation: 'Encapsulation = control access to fields using access modifiers + getters/setters.',
  },
  {
    id: 33,
    checkpoint: 'oop',
    difficulty: 3,
    subskill: 'override-vs-overload',
    prompt: 'Which is true about method overriding?',
    optionA: 'Same name, different parameters',
    optionB: 'Same name + same parameters in subclass, different implementation',
    optionC: 'Same name, different return type only',
    optionD: 'Only static methods can be overridden',
    correctOption: 'B',
    explanation: 'Overriding replaces a superclass method in a subclass with same signature.',
  },
  {
    id: 34,
    checkpoint: 'oop',
    difficulty: 3,
    subskill: 'abstract-class',
    prompt: 'Which statement about abstract classes is correct?',
    optionA: 'You can instantiate an abstract class directly',
    optionB: 'Abstract classes cannot have constructors',
    optionC: 'Abstract classes can have abstract and non-abstract methods',
    optionD: 'Abstract classes must contain only abstract methods',
    correctOption: 'C',
    explanation: 'Abstract classes can mix implemented + abstract methods and still have constructors.',
  },
];
