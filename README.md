# tree-based-quorum-system

How To Run:
1) Move OSProject 2 into working directory
2) make (using makefile)
3) Run in order S1 S2 S3 S4 S5 S6 S7 C1 C2 C3 C4 C5
4) S0 needs to run on dc20
    1) S1 needs to run on dc21
    2) S2 needs to run on dc22
    3) S3 needs to run on dc23
    4) S4 needs to run on dc24
    5) S5 needs to run on dc25
    6) S6 needs to run on dc26
    7) S7 needs to run on dc27
    8) C1-C5 can be run on any of the machines
5) It will either print the completion statements for each client or it wont.
In the case that it didnt the program reached a deadlock. (Is easy to notice)

My Servers are in a tree structure. Requests and replies are propogated through the tree.
The leaf nodes tend to send very little request due to being dominated by the higher nodes.
The amount sent is also affected by my Quorums.
For some reason server 3 completes the first request from clients and send the most requests.
