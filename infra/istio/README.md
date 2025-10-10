# Istio Ambient Deployment Notes

1. Install Istio operator:
   ```bash
   istioctl operator init
   ```
2. Apply ambient profile manifest:
   ```bash
   kubectl apply -f ambient-profile.yaml
   ```
3. Enable ambient for namespaces:
   ```bash
   kubectl label namespace orders istio.io/dataplane-mode=ambient
   kubectl label namespace payments istio.io/dataplane-mode=ambient
   ```
4. Deploy waypoint proxies for HTTP services:
   ```bash
   kubectl apply -f waypoint-orders.yaml
   ```
5. Verify mesh health with `istioctl proxy-status` and Grafana dashboards.
