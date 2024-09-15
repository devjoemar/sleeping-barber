
### **Sleeping Barber Problem - Spring Boot, Java 21 Virtual Threads, and WebSockets**

Welcome to the **Sleeping Barber Problem** demonstration, a classic concurrency problem re-imagined using the latest technologies: **Spring Boot**, **Java 21 Virtual Threads**, and **WebSockets**. This project illustrates how to solve the problem using modern concurrency primitives and real-time communication.

---

### **Table of Contents**
- [Problem Statement](#problem-statement)
- [Solution Overview](#solution-overview)
- [Technologies Used](#technologies-used)
- [How We Approached the Solution](#how-we-approached-the-solution)
  - [Java 21 Virtual Threads](#java-21-virtual-threads)
  - [Concurrency with Semaphores](#concurrency-with-semaphores)
  - [WebSockets for Real-Time Updates](#websockets-for-real-time-updates)
- [Conclusion](#conclusion)

---

### **Problem Statement**

The **Sleeping Barber Problem** is a classic synchronization problem that illustrates the challenges of managing resources in a concurrent system. Here's how the problem works:

- A barbershop has a fixed number of barbers and barber chairs, as well as a limited number of waiting room chairs for customers.
- If a barber is available, the customer gets a haircut. If not, the customer waits in the waiting room.
- If all waiting chairs are full, customers leave the barbershop without getting a haircut.
- Barbers sleep when there are no customers, and wake up when a customer arrives.

The challenge is to ensure that customers are served efficiently, barbers do not remain idle unnecessarily, and that we avoid deadlocks and race conditions.

---

### **Solution Overview**

This project demonstrates a solution to the Sleeping Barber Problem using **Java 21 Virtual Threads** for lightweight concurrency, **Spring Boot** for building a web-based service, and **WebSockets** to provide real-time updates to the client.

#### **Key Features of the Solution:**

- **Concurrency with Java 21 Virtual Threads:** Virtual threads enable us to create a large number of concurrent threads without significant overhead, which is perfect for simulating the activities of barbers and customers.
- **Synchronization Using Semaphores:** We use semaphores to manage access to shared resources, such as the barber chairs and the waiting room. This ensures that customers and barbers interact with each other in a safe, synchronized manner.
- **Real-Time Updates with WebSockets:** To make the system interactive, we use WebSockets to push real-time updates to the client, such as when a customer enters, gets a haircut, or leaves the shop.
- **Spring Boot Integration:** This project is built using Spring Boot, making it easy to set up and run, while providing the robust infrastructure needed for a web application.

---

### **Technologies Used**

- **Java 21**: We leverage the latest concurrency features with **Virtual Threads** to handle large numbers of customer and barber interactions efficiently.
- **Spring Boot 3**: Provides the web application framework, making it easy to develop, run, and manage the application.
- **WebSockets**: Used to deliver real-time updates to clients, allowing them to see the barbershop activity in real-time.
- **Semaphore**: Java's semaphore class helps control access to shared resources (barber chairs and waiting room seats).
- **SLF4J with Logback**: For logging and monitoring what's happening behind the scenes.

---



### **How We Approached the Solution**

#### **Java 21 Virtual Threads**

With the introduction of **Virtual Threads** in Java 21, we can now manage thousands of lightweight threads in our system. Traditional threads are expensive in terms of system resources, but virtual threads are lightweight and more efficient.

**Why did we use virtual threads?**
- **Scalability:** Virtual threads enable us to scale the number of barbers and customers without worrying about system overhead.
- **Concurrency:** By leveraging virtual threads, each customer and barber operates in its own thread, allowing for real-time, concurrent processing.

In our implementation, each barber and customer is run in a separate virtual thread, allowing them to operate independently but safely interact using semaphores.

#### **Concurrency with Semaphores**

To manage the limited number of barber chairs and waiting room seats, we use semaphores to handle access to these resources:

- **Barber Chairs Semaphore:** Controls the number of customers that can be seated in a barber chair at a time.
- **Waiting Room Mutex:** Ensures that only one customer can check the availability of waiting chairs at a time.

**Why use semaphores?**
- **Prevent Race Conditions:** Semaphores ensure that multiple threads (customers and barbers) don’t access shared resources simultaneously, avoiding conflicts.
- **Controlled Resource Access:** By using semaphores, we ensure that only a fixed number of customers can be seated or wait at any given time.

#### **WebSockets for Real-Time Updates**

To make the system more interactive, we chose **WebSockets** to push updates in real-time to the client. This allows the client to see when customers enter, sit down for a haircut, or leave the barbershop.

**Why WebSockets?**
- **Real-Time Feedback:** WebSockets allow us to push updates to the client as soon as an event occurs (e.g., customer leaves, haircut finishes).
- **Better User Experience:** It provides a dynamic, live experience where the user doesn’t need to refresh the page to see the latest activity.

We implemented a WebSocket event publisher (`EventPublisher.java`) that broadcasts real-time updates to subscribed clients whenever significant events occur in the barbershop.

---

### **Conclusion**

This project demonstrates how to solve a classic concurrency problem using the latest advancements in Java, specifically **Virtual Threads** introduced in Java 21. By integrating **Spring Boot** and **WebSockets**, we not only solve the synchronization problem but also provide real-time feedback, making the system more interactive and engaging.

The **Sleeping Barber Problem** is a great way to showcase how modern tools like virtual threads can simplify complex concurrency issues. With the addition of WebSockets, we bring the solution to life by showing real-time updates to the users. This project is intended as a demonstration, but the principles and techniques shown here can be applied to real-world scenarios where resource management and concurrency are critical.

Feel free to fork the project, experiment with it, or adapt it to your needs!

---

