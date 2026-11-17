# Testing an Access Rule, and Making the Test Reusable

**Time:** 60 minutes: ~5 min framing, ~35 min working, ~20 min discussion.
**Stack:** the project is Java (JDK 17+) with Playwright and JUnit. You may use any tool you like to write your check.
**AI tooling:** allowed and expected. See [How we work](#how-we-work).

## Context

A ComPsych team owns a small account portal: a web page backed by a JSON API, where staff can
view and edit member accounts. The team has just shipped this product change:

> **Only admins can update accounts.**

The team's end-to-end suite is green, and the change has gone out. You've been asked, as the SDET
who supports this team, to look at how well the suite protects that rule.

**This is a hypothetical scenario.** Nothing you write here will be used by ComPsych or any team;
this is an interview exercise, and the app and the other teams below are made up. Imagine that the
team wants the rule protected now, and that other teams would have the same kind of rule to protect,
so your approach should be easy for them to adopt.

In this scenario, the endpoints are standardized across ComPsych: every team's app has the same
login and account routes as this one. The individual roles are not standardized. Each team has its own set of
roles and its own rules about who may do what.

## What you're given

| Path | Contents |
|---|---|
| `app/` | The portal: a Javalin service and one HTML page. `app/README.md` describes how it is meant to behave and is the spec you test against. Mock data only. |
| `e2e/` | The team's Playwright (Java) + JUnit suite, run with `e2e/run-e2e.sh` (`e2e\run-e2e.cmd` on Windows). |

To run everything, from `e2e/`:

```
./run-e2e.sh                          # starts the app, runs the suite, stops the app
./run-e2e.sh -Dtest=AccountApiTest    # extra arguments go to Maven
```

The first run downloads Maven, dependencies and a browser, so it needs the network once and takes a
few minutes. When a UI test fails, a screenshot and a Playwright trace are written to
`e2e/target/artifacts/`, and the app's own log is `e2e/target/app.log`. See `e2e/README.md` for
the details.

The app and the suite are separate projects. The suite drives a running app over HTTP and in a
browser, and shares no code with it.

## What to do

We want a check that is built to be reused **from the start**, not a one-off that you tidy up
afterwards. Keep that in mind from your first line.

1. **Find the highest-risk gap.** Look at what the suite does and does not check, compared with
   the product change. Tell us what you found, which gap you would close first, and why it
   outranks the others.
2. **Implement and run one authorization check** for that gap, in `e2e/` or wherever you think it
   belongs. Design it to be reusable across roles from the start, for example with a fixture for
   signing in as a given role. You choose what it checks and how it's written.
3. **Show that the check can catch a failure.** A passing check proves little by itself. Show us
   that it can fail, and what it fails on in this app.
4. **Demonstrate that it can be reused.** Show that your check works for roles it wasn't written
   for, for example by adding a new role (real or made up) or a second test that uses the same
   fixture, without rewriting what you built. Keep it small: we want to see that the design
   holds, not a finished framework.

You will not finish everything you could do in the time, and that's by design. Pick what matters,
and say why.

## Discussion

In the last part of the session we'll talk about the work, and we'll ask you things like:

- In the scenario, how you would share your fixture across other teams' repos, and what a team with
  a different set of roles would have to supply.
- Where in a delivery pipeline the check should run, and whether a failure should block a merge.

If you have time left after the build, there is also an optional follow-up. One UI test in the
suite fails on some runs and passes on others, with no code change. What evidence would you
collect to find out why, and what would you do about the test? You don't have to fix it.

## What you don't need to build

A CI configuration file, a framework for other teams to install, or a rewrite of the existing
suite. You don't need to change the app. If you think the app should change, say so.

## How we work

**Use AI tools as much as you like.** We're evaluating how you direct them, check what comes
back, and decide what to trust, not how fast you type.

Please think out loud. We want to hear:

- How you decide which gap matters most.
- What you hand to the AI, and what context you give it.
- What you checked yourself rather than taking the AI's word for it, and what you found.
- How you know your check would catch the problem it is meant to catch.
- How reuse shaped your design from the start, and the trade-offs in the reusable part.

It's fine to pause and think. Silence for ten minutes is the only thing that costs you.

## Questions

Ask them. Some things in this brief are deliberately open, and we'd rather you notice and decide
than quietly assume.
