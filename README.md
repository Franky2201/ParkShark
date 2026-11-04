# ParkShark

![ParkShark logo](parkshark.png)

ParkShark is a totally legit pay-to-use parking lot company.
Their goal is to take over the (underground) world of parking lots. They first tried to accomplish this by using violence.
However, they figured a mobile app that automates their processes might also get the job done.
As a bonus, that solution will involve less killing. This mobile-app will be used by both ParkShark customers 
and ParkShark managers.

The front-end of ParkShark will be created by a freelancer who they'll never pay (that's what the *free* in freelancer stands for after all).

The front-end (mobile app) of ParkShark will communicate with the back-end of ParkShark using an HTTP(S) implementation of REST 
using JSON as its message format. This back-end will contain the entire domain and all the business logic.
It's your job to implement this back-end.

## Technical requirements for JAVA

- Create a new GitHub repository (one per team)
    - Think about the files Git should ignore, do this before making your first commit...
- Provide a REST(ful) Web API (with JSON as the message / body format)
- Use Spring Boot or Quarkus (latest (LTS) release)
- Use Maven
- Setup a Jenkins or Travis or GitHub Actions for continuous integration
    - We'll help you with this (but honestly, it's really up to you to configure it!)...
- Perform logging (use spring-boot-starter's logging dependencies: logback and slf4j or read [this tutorial](https://quarkus.io/guides/logging) if you're using Quarkus)
    - Certainly log all interactions with the application that can be defined as "errors"
        - E.g.: unauthorized access, illegal arguments, exceptions in general,...
- Include OpenAPI using Swagger(UI) to provide a readable documentation/manual of your REST(ful) Web API
- use JPA (Hibernate or EclipseLink) in combination with a PostgreSQL Database to store and access the data.
    - Setup a proper test configuration, which runs the integration tests against an in-memory database (e.g. H2)
        - Make it a separate technical story.
    - Correctly setup and handle the transactions.
    - Write your DDL (create tables,...) in a separate `.sql` file, which you also put under version control.
- Think about security (Basic authentication or JWT), though it doesn't have to be a priority.
    - Maybe make it a separate technical story.
    - You can use our keycloak server (https://keycloak.switchfully.com)

## Technical requirements for .NET

- Create a new GitHub repository (one per team) and add the teachers to it
- Use REST (with JSON as the message / body format)
- Use ASP.NET Core WebApi
- Use Entity Framework Core
- Use Azure DevOps Build pipeline for continuous integration
    - We'll help you with this
- Perform logging (use logging provided by .NET Core) 
    - Certainly log all interactions with the application that can be defined as "errors" - E.g.: unauthorized access, illegal arguments, exceptions in general,...
    - Also log all database queries done via EF Core
- Use Swagger to provide a readable document of your WebApi
- Use MS Sql Server for a database
- Don't bother writing EF Core migrations, you can compose you database schema migrations in T-SQL
    - Do make sure to include your T-SQL changes in a Scripts folder in the GitHub Repo, so that we can inspect the migrations done to the DB
- Setup a transaction per HTTP call (we can help you with this)

## Architectural Setup

You can choose for yourself if you want to work with a multi- or single-module project. Do remember that you have four days in total for this project and multi-module can bring unnecessary complexities to the table. We advise you to only go for this approach when feeling very confident.   

Guidelines for a multi-module project:

Provide a multi-module setup where each module (not package) represents a 'layer'.
- Although a Layered architecture might not be the best architecture (for big projects), when used properly(!), it is actually 
a nice way of separating concerns in your code. Read the following links:
    - https://www.codingthearchitecture.com/2016/04/25/layers_hexagons_features_and_components.html
    - http://www.codingthearchitecture.com/2015/03/08/package_by_component_and_architecturally_aligned_testing.html
- We're proposing 3 (to 5) layers: war (top-level, optional), api, service, domain and infrastructure (lowest-level, optional)
    - But, at the very least provide an api, service and domain module.
    - The war module (optional) can be used to package together the whole application (also contains the Main Class)
    - The infrastructure module (optional) can be used to contain code on which all other modules can depend.
- Think about a proper package strategy
    - What's the base package for every module?
    - Do you package per feature or per technical component, or a combination of both?
    
Furthermore, use DTOs for encapsulating the payload of a request or of a response to/of your Web API. 
(and ideally a different DTO for the request and for the response of a call to your Web API)
- https://martinfowler.com/eaaCatalog/dataTransferObject.html  

## Project requirements

The Parkshark project is supposed to be organized as a project following the SCRUM methodology.

- We want to see a clear visualized **product backlog**
- We want to see a **project kickoff** followed by **2 sprints** of two days.
- At the start of the sprint we want to see a **sprint kickoff meeting** defining the **sprint backlog** (estimated and prioritized) of that sprint.
- We want to see a clear visualized sprint backlog and up-to-date kanban board 
- We want to see **daily standups** (at least once a day on fixed timeslots) discussing the current sprint progress by using a kanban board
- We want to see a **sprint review** (=demo) and **retrospective** at the end of the project
- In your team's Slack channel, we want to see a link to:
    - kanban board
    - team calendar (daily standup, sprint kickoff, scrumboard, whiteboard, ...)

**Project kickoff guidelines:**
- Choose a team name and create a Slack channel for your team
- Define team rules
    - Daily standup at X
    - Scrummaster role (fixed, rotating)
    - Ownership of stories
    - Pairing/repairing
    - Code review sessions
- Review/discuss the project (board session)
    - General understanding of the functionality (stories)
    - General understanding of the technical set-up
    - Domain modelling 
- Create a Kanban board (Trello) to visualize work
    - Define the process steps
    - Define the transitions (DoD)
    - Add tasks to the board (color coded, prioritized)
        - Functional tasks (stories)
            - Technical grooming
            - Define subtasks
		- Non-functional tasks
		    - technical setup
            - Github repository (add Switchfully trainers!)
            - Setup communication channels
		- Questions?
 - Setup a shared drive to share useful documentation
    - domain model, detail of stories, team rules, ...
- Don't forget to add necessary information to your Slack channel (Kanban board, team calendar, ...)

## Timing

We've created two categories of stories: Must-Have's and Nice-To-Have's. 
Focus on the Must-Have's, when they're fully implemented (or, to not interfere with other developers) you can implement the Nice-To-Have's.
    
## Functional Stories

The functional requirements are written down as stories.

- The functional analyst will be available to answer all your questions (he'll speak for the customer ParkShark)
- The functional analyst made some design decisions, if you want to challenge those, you always can. Come prepared though. 😄

### Story DI1: Create a Division
**As a Manager I want to create a division.**
> ParkShark became the company it is by doing takeovers of competing companies. 
These companies were never fully merged with ParkShark, they became divisions.
- A division has a name, an original name (the original name of the bought company) and a director
- Prioritization: Must-Have

### Story DI2: Get all Divisions
**As a Manager I want to get an overview of all divisions.**
- Prioritization: Must-Have

### Story DI3: Get a Division
**As a Manager I want to get a single, specific, division.**
- Prioritization: Nice-To-Have

### Story DI4: Create a Subdivision
**As a Manager I want to create a division that is a subdivision of an already existing division**
> Sometimes, a company that was bought by ParkShark, bought some companies itself. We want to be able to express that in our application by creating subdivisions.
- A subdivision is the same as a division, it simply has another division that functions as its parent.
- Prioritization: Must-Have
    
### Story PL1: Create a Parking lot
**As a Manager I want to create a parking lot.**
- A parking lot has a name, a category, a (max) capacity, a contact person, and an address
- A parking lot also has a price per hour for car allocation (parking your car)
- The contact person has a name, a mobile phone number, a telephone number, an email, and an address
    - As a means of contact: 
        - A contact person should always have a (valid) email address.
        - A contact person should always have a mobile phone number, or a telephone number. Both are allowed, but at least one is always required!
- The address consists of a street name, street number, and a postal code
    - A postal code consists of an actual postal code and a label (e.g. *3000, Leuven*)
- A category is currently restricted to either *underground building* or *above ground building*. However, there can be more in the future (e.g. *street*, *open space*,...).
- Prioritization: Must-Have

### Story PL2: Get all Parking lots
**As a Manager I want to get an overview of all parking lots.**
- The returned parking lots should include the id, name, capacity and the contact person's email + telephone (nothing else)
- Prioritization: Must-Have

### Story PL3: Get a Parking lot
**As a Manager I want to get a single, specified, parking lot.**
- The returned parking lot should include all the data we have of a single parking lot
- Prioritization: Nice-To-Have

### Story DIPL1: Parking lot belongs to a Division
**As a Parking lot I want to have a Division so that I know to what division I belong**
- As of now, every parking lot should have a division when being created, whenever a specific parking lot is returned, it should
also include the information of the division.
    - This story impacts Story PL1 and PL3 (not PL2)
- Prioritization: Must-Have 

### Story ME1: Register as a Member
**As a Person I want to register myself as a Member so that I can park my car in a parking lot of ParkShark.**
> In order to use the ParkShark parking lots, a person needs to become a member first.
- A member has personal information (name, address, contact information,...), 
    - A name (first, last)
    - An address
    - A telephone number
    - An Email address
    - A license plate (license plate number and issuing country) 
    - A registration date.
        - A technical tip: In Java, use the new Java Time API. For JPA mapping, check the following link: https://www.baeldung.com/jpa-java-time
- Prioritization: Must-Have

### Story ME2: get all Members
**As a Manager I want to get an overview of all Members**
- The returned members should contain the id, name, license plate (just the number), telephone number, email-address and registration date (nothing else) 
- Prioritization: Must-Have

### Story ME3: get a Member
**As a Manager I want to get a single, identified, Member**
- The returned member should include all the data we have of a single member
- Prioritization: Nice-To-Have

### Story ML1: Select a Membership level
**As a Member I want to select a Membership level.**
> By selecting a certain membership level, a member will gain certain advantages.
- In total, there are 3 membership levels: **bronze**, **silver** and **gold**.
- All membership levels come with a same set of options, however, their values differ:
    - Bronze
        - Monthly cost: €0
        - Parking spot allocation reduction: 0%
        - Maximum allowed allocation-time of parking spot: 4 hours
    - Silver
        - Monthly cost: €10
        - Parking spot allocation price-per-hour reduction: 20%
        - Maximum allowed allocation-time of parking spot: 6 hours
    - Gold
        - Monthly cost: €40
        - Parking spot allocation price-per-hour reduction: 30%
        - Maximum allowed allocation-time of parking spot: 24 hours 
- Extend the Member registration so that upon registering a member we can choose to provide a membership level. However,
it should still be possible to not provide one. In that case, we'll automatically set the membership level to **bronze.**
- It might be possible that extra options or entire membership levels are introduced in the future. Those changes don't have to happen dynamically. 
It's okay if they only take effect after a new deploy to production.
- There are multiple ways of implementing this...
- Prioritization: Must-Have

### Story PSA1: Start Allocating a Parking spot
**As a Member I want to start allocating a Parking spot so that I can enter the parking lot and park my car.**
> Parking spot allocation is the term they use at ParkShark to indicate that a 
member is in front of a parking lot which he want to enter so he can park his car. The member can use the mobile app to indicate
he wants to enter the parking lot. This is what ParkShark calls "starting Parking spot allocation".
- To start allocating a Parking spot, the following info is required:
    - The member (id)
    - The license plate number (of the actual car being parked)
    - The parking lot (id)
- Starting a parking allocation only succeeds if the following requirements are met:
    - The member is recognized by the system
    - The provided license plate number is the same as the member's license plate number
        - It can be different, but only if the membership level is Gold
    - The parking lot is recognized by the system
    - The parking lot still has available capacity (it's not full)
- Make sure that the (started) parking allocation is uniquely identifiable (hint).
    - Make sure that the member who started the parking allocation receives this id.
- A parking allocation should have a start time (when did the member start the allocation?)
- Prioritization: Must-Have

### Story PSA2: Stop Allocating a Parking spot
**As a Member I want to stop allocating a Parking spot so that I can leave the parking lot**
> When a member wants to leave a parking lot (and thus has just left his parking spot) he has to use the mobile app to
indicate this. ParkShark calls this "stopping Parking spot allocation".
- To stop a parking allocation, the following info is required:
    - The parking allocation id
    - The Member (id), (if you use security, you can check this based on the logged-in user)
        - Spring: https://www.javacodegeeks.com/2018/02/securitycontext-securitycontextholder-spring-security.html
        - Quarkus: 
          - give your endpoint an additional parameter `@Context SecurityContext securityContext`
          - in your method you now have access to `securityContext.getUserPrincipal()`
- A parking allocation can only be stopped, if it's active.
- A parking allocation can only be stopped, if the provided member (id) is the owner of the allocation.
- When an allocation is stopped, a stop time should be added to the allocation.
    - Allocations that have a stop time can be seen as 'passive' / 'stopped' parking allocations. Parking allocations without a stop time can be seen
    as active allocations. Do make this explicit!
        - Thus, every parking allocation should have a status (We'll refer to it as the parking allocation status: active or stopped (for now)). 
- Prioritization: Must-Have
    
### Story PSA3: get all parking allocations
**As a Manager I want to get an overview of all parking allocations**
- By default all allocations are returned, ordered on start time ascending
- The following filters should be available:
    - limit the returned number of allocations to the provided amount (e.g. limit to 100 results)
    - filter between all allocations, all active allocations and all stopped allocations (filter based on parking allocation status)
    - ordering can be set to descending (or ascending)
- Prioritization: Must-Have

### Story PSA4: get all parking allocations for a Member
**As a Manager I want to get an overview of all parking allocations for a given Member**
- The following filter should be available:
    - filter between all allocations, all active allocations and all stopped allocations
- Prioritization: Nice-To-Have

### Story PSA5: get all parking allocations for a Parking lot
**As a Manager I want to get an overview of all Allocations for a given Parking lot**
- The following filter should be available:
    - filter between all allocations, all active allocations and all stopped allocations
- Prioritization: Nice-To-Have

### Story PSA6: get all parking allocations (Duration)
**As a Manager I want to get an overview of all parking allocations, but also showing the duration of each allocation**
- Builds on Story **PSA3**
- Each allocation returned (when getting all Allocations) should also include the duration in `hh:mm:ss` of the allocation (`duration = start time - stop time`)
    - If the stop time is not yet filled in, we use the current time.
- Prioritization: Nice-To-Have

### Story INV1: get monthly Invoice 
**As a Member I want to get my monthly Invoice so that I can pay it.** (otherwise ParkShark will come and break my legs :scream: )
> When a member requests his monthly invoice, an invoice will be generated on the fly and returned to the member. All parking allocations 
with status stopped should be collected and used (you don't have to check the current month, since we'll generate an invoice every month). 
- To get the latest invoice, the following info is required:
    - The member (id)
- Each parking allocation with status **passive** needs to be transformed to an Invoice Item
    - Each of those parking allocations will then get status **invoiced**
- An Invoice Item has
    - A (reference to a) parking allocation
    - A calculated price
        - The price is calculated as follows:
            - `Total number of whole hours parked (rounded up) * cost per hour (defined in parking lot)`
                - E.g. Parked for 2h:20m will be charged as 3h.
                - If any reduction applies (based on Membership level), make sure to apply it!
            - If the total number of whole hours parked (rounded up) exceeds the Maximum allowed allocation-time of parking spot 
            (based on Membership level), a fine of €2.5 per hour is added to the price.
- An Invoice has
    - A unique reference number
    - A creation date
    - An expiration date
    - A collection of Invoice items (or should an Invoice Item contain a reference to Invoice?)
    - A status (Open or Closed)
        - If Closed, it should have a "date of payment"
- The returned invoice should also contain the total price to pay. This total price consists of:
    - The sum of the price of each Invoice Item (+ fine)
    - The membership monthly cost
- Prioritization: Nice-To-Have

### Story INV2: get all Invoices
**As a Manager I want to get an overview of all Invoices.**
- Prioritization: Nice-To-Have

### Story INV3: Mark an Invoice as Closed
**As a Manager I want to mark an Invoice as closed**
- To mark an invoice as closed, the following info is required:
    - The invoice unique reference
- The Invoice's status should be set to closed.
- Prioritization: Nice-To-Have

