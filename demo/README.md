# Demo data for screenshots

This is realistic sample code for an order-processing report (customers,
orders, a monthly statement) — not a real company, just a stand-in
("acmecorp") so the screenshot looks like a real project instead of a toy
example.

## How to get the screenshot

1. Launch the plugin sandbox from the `n-plus-one-query-companion` folder:
   `./gradlew runIde`
2. In the sandbox IDE, open this `demo/` folder as the project (or open
   `MonthlyStatementReport.java` directly).
3. Enter Full Screen (`View > Appearance > Enter Full Screen`, or search
   "Enter Full Screen" via Find Action).
4. You should see a small warning icon in the left gutter, right next to
   the `for (Customer c : customers)` line. Hover it — the tooltip names
   the `orders` association and `@OneToMany` annotation.
5. Take the screenshot (`Win+Shift+S` or your usual tool) with that icon
   and its tooltip visible, and save it directly into
   `n-plus-one-query-companion/docs/screenshots/`.
6. Close the sandbox window when done.
