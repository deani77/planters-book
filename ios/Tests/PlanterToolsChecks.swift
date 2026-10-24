import Foundation
func close(_ actual: Double?, _ expected: Double) { precondition(actual != nil && abs(actual! - expected) < 1e-8) }
close(PlanterCalculations.density(distance: 10, unit: .metres, pattern: .square)?.palmsPerHectare, 100)
close(PlanterCalculations.density(distance: 9, unit: .metres, pattern: .triangular)?.palmsPerHectare, 142.5556220221309)
let feet = PlanterCalculations.density(distance: 30, unit: .feet, pattern: .triangular)!
let metres = PlanterCalculations.density(distance: 9.144, unit: .metres, pattern: .triangular)!
close(feet.palmsPerHectare, metres.palmsPerHectare)
let liquid = PlanterCalculations.dose(rate: 2, unit: .litres, tankLitres: 16, sprayLitresPerHectare: 200)!
close(liquid.productPerPump, 160); close(liquid.hectaresPerPump, 0.08); close(liquid.pumpsPerHectare, 12.5)
close(PlanterCalculations.dose(rate: 2000, unit: .millilitres, tankLitres: 16, sprayLitresPerHectare: 200)?.productPerPump, 160)
close(PlanterCalculations.dose(rate: 1.5, unit: .kilograms, tankLitres: 20, sprayLitresPerHectare: 400)?.productPerPump, 75)
close(PlanterCalculations.dose(rate: 1500, unit: .grams, tankLitres: 20, sprayLitresPerHectare: 400)?.productPerPump, 75)
for value in [0.0, -1.0, Double.infinity, Double.nan] {
    precondition(PlanterCalculations.density(distance: value, unit: .feet, pattern: .triangular) == nil)
    precondition(PlanterCalculations.dose(rate: value, unit: .litres, tankLitres: 16, sprayLitresPerHectare: 200) == nil)
    precondition(PlanterCalculations.dose(rate: 2, unit: .litres, tankLitres: value, sprayLitresPerHectare: 200) == nil)
    precondition(PlanterCalculations.dose(rate: 2, unit: .litres, tankLitres: 16, sprayLitresPerHectare: value) == nil)
}
precondition(PlanterCalculations.dose(rate: 201, unit: .litres, tankLitres: 16, sprayLitresPerHectare: 200) == nil)
precondition(PlanterCalculations.input("9,5") == nil)
precondition(PlanterCalculations.input("1,000") == 1000)
precondition(PlanterCalculations.input("2,000.50") == 2000.5)
for text in ["", "0", "-1", "nan", "infinity", "1,00"] { precondition(PlanterCalculations.input(text) == nil) }
precondition(PlanterCalculations.number(0.00001) != "0.00")
print("Planter calculators: geometry, feet conversion, rate-unit conversions, tank coverage and invalid-input checks passed")

let fertilizer = PlanterCalculations.fertilizer(palms: 100, kilogramsPerPalm: 5, bagKilograms: 50)!
close(fertilizer.kilograms, 500); close(fertilizer.tonnes, 0.5); close(fertilizer.wholeBags, 10); close(fertilizer.balanceKilograms, 0)
let partial = PlanterCalculations.fertilizer(palms: 7, kilogramsPerPalm: 2, bagKilograms: 5)!
close(partial.equivalentBags, 2.8); close(partial.wholeBags, 3); close(partial.balanceKilograms, 1)
precondition(PlanterCalculations.fertilizer(palms: 2.5, kilogramsPerPalm: 2, bagKilograms: 50) == nil)
close(PlanterCalculations.calibration(waterLitres: 2, testAreaSquareMetres: 100), 200)
close(PlanterCalculations.calibration(waterLitres: 4, testAreaSquareMetres: 200), 200)
let cost = PlanterCalculations.workCost(labourRM: 300, materialRM: 150, otherRM: 50, quantity: 10, basis: .hectares)!
close(cost.totalRM, 500); close(cost.rateRM, 50)
close(PlanterCalculations.workCost(labourRM: 0, materialRM: 0, otherRM: 0, quantity: 10, basis: .tonnes)?.rateRM, 0)
precondition(PlanterCalculations.workCost(labourRM: 1, materialRM: 0, otherRM: 0, quantity: 0.5, basis: .palms) == nil)
close(PlanterCalculations.convert(1, from: .hectares, to: .acres), 2.4710538146716534)
close(PlanterCalculations.convert(1, from: .acres, to: .squareMetres), 4046.8564224)
close(PlanterCalculations.convert(1, from: .feet, to: .metres), 0.3048)
close(PlanterCalculations.convert(1000, from: .kilograms, to: .tonnes), 1)
close(PlanterCalculations.convert(0, from: .hectares, to: .acres), 0)
precondition(PlanterCalculations.convert(1, from: .metres, to: .kilograms) == nil)
for group in ConversionGroup.allCases {
    for from in group.units { for to in group.units {
        let converted = PlanterCalculations.convert(123.456, from: from, to: to)!
        close(PlanterCalculations.convert(converted, from: to, to: from), 123.456)
    } }
}
for bad in [0.0, -1.0, Double.nan, Double.infinity] {
    precondition(PlanterCalculations.fertilizer(palms: 100, kilogramsPerPalm: bad, bagKilograms: 50) == nil)
    precondition(PlanterCalculations.fertilizer(palms: 100, kilogramsPerPalm: 5, bagKilograms: bad) == nil)
    precondition(PlanterCalculations.calibration(waterLitres: 2, testAreaSquareMetres: bad) == nil)
    precondition(PlanterCalculations.workCost(labourRM: 1, materialRM: 0, otherRM: 0, quantity: bad, basis: .hectares) == nil)
}
for bad in [-1.0, Double.nan, Double.infinity] {
    precondition(PlanterCalculations.workCost(labourRM: bad, materialRM: 0, otherRM: 0, quantity: 1, basis: .hectares) == nil)
    precondition(PlanterCalculations.convert(bad, from: .metres, to: .feet) == nil)
}
precondition(PlanterCalculations.input("0", allowZero: true) == 0)
print("Additional tools: bag rounding, calibration scaling, cost bases, conversion round trips and invalid inputs passed")
