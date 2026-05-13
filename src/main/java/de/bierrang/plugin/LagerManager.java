// Bug 2 Fix: kein frühzeitiges return – alle Slots/Nodes pro Zyklus verarbeiten
private void tryTransfer() {
    if (inputs.isEmpty() || outputs.isEmpty()) return;

    List<LagerNode> outCopy = new ArrayList<>(outputs);
    List<LagerNode> inCopy  = new ArrayList<>(inputs);

    for (LagerNode outNode : outCopy) {
        Container outContainer = getContainerAt(outNode.getLocation());
        if (outContainer == null) continue;

        Inventory outInv = outContainer.getInventory();

        for (int slot = 0; slot < outInv.getSize(); slot++) {
            ItemStack stack = outInv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            if (!passesFilter(outNode, stack.getType())) continue;

            boolean moved = moveToAnyInput(inCopy, stack.clone());
            if (moved) {
                outInv.setItem(slot, null);
                needsSave = true;
                // kein return → nächste Slots und Nodes werden weiterverarbeitet
            }
        }
    }
}

// Bug 3 Fix: Leere Filterliste = alles erlaubt
private boolean passesFilter(LagerNode node, Material mat) {
    if (node.getFilterMaterials().isEmpty()) return true;
    boolean contains = node.getFilterMaterials().contains(mat);
    return node.isWhitelist() == contains;
}
